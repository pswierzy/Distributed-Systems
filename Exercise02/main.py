from fastapi import FastAPI, Form, HTTPException
from fastapi.responses import HTMLResponse
from fastapi.staticfiles import StaticFiles
from datetime import datetime
import httpx
import asyncio
import os
from dotenv import load_dotenv

app = FastAPI(title="Kalkulator Podróżnika")

app.mount("/static", StaticFiles(directory="static"), name="static")

@app.get("/", response_class=HTMLResponse)
async def read_root():
    with open("static/index.html", "r", encoding="utf-8") as f:
        return HTMLResponse(content=f.read())

load_dotenv()
RAPIDAPI_KEY = os.getenv("RAPIDAPI_KEY")

if not RAPIDAPI_KEY:
    raise RuntimeError("BRAK KLUCZA RAPIDAPI!")

HEADERS_AIR = {
    "X-RapidAPI-Key": RAPIDAPI_KEY,
    "X-RapidAPI-Host": "sky-scrapper.p.rapidapi.com"
}
HEADERS_BOOKING = {
    "X-RapidAPI-Key": RAPIDAPI_KEY,
    "X-RapidAPI-Host": "booking-com15.p.rapidapi.com"
}

# Szukanie kalendarzy lotów

async def fetch_calendar(origin: str, dest: str, from_date: str):
    url = "https://sky-scrapper.p.rapidapi.com/api/v1/flights/getPriceCalendar"
    params = {"originSkyId": origin, "destinationSkyId": dest, "fromDate": from_date, "currency": "PLN"}
    
    async with httpx.AsyncClient(timeout=15.0) as client:
        res = await client.get(url, headers=HEADERS_AIR, params=params)
        
        if res.status_code != 200: 
            raise HTTPException(status_code=502, detail=f"Błąd HTTP {res.status_code} z Air Scraper: {res.text}")
            
        data = res.json()
        
        if data.get("status") is False:
            raise HTTPException(status_code=502, detail=f"API odrzuciło zapytanie: {data}")
            
        days = data.get("data", {}).get("flights", {}).get("days", [])
        return [{"date": d.get("day"), "price": float(d["price"])} for d in days if d.get("price")]

# Znajdywanie hoteli

async def fetch_city_name_from_iata(iata_code: str):
    url = "https://booking-com15.p.rapidapi.com/api/v1/flights/searchDestination"
    params = {"query": iata_code}
    
    async with httpx.AsyncClient(timeout=10.0) as client:
        res = await client.get(url, headers=HEADERS_BOOKING, params=params)
        if res.status_code != 200:
            raise HTTPException(status_code=502, detail="Błąd pobierania nazwy miasta (Booking).")
        
        data = res.json().get("data", [])
        if not data:
            raise HTTPException(status_code=404, detail=f"Nie znaleziono lotniska dla kodu {iata_code}")
        
        city_name = data[0].get("cityName", "")
        if not city_name:
            city_name = data[0].get("name", "")
        return city_name

async def fetch_booking_dest_id(city_name: str):
    url = "https://booking-com15.p.rapidapi.com/api/v1/hotels/searchDestination"
    params = {"query": city_name}
    
    async with httpx.AsyncClient(timeout=10.0) as client:
        res = await client.get(url, headers=HEADERS_BOOKING, params=params)
        
        if res.status_code != 200:
            raise HTTPException(status_code=502, detail="Błąd Booking.com (Szukanie miasta).")
            
        data = res.json().get("data", [])
        
        for item in data:
            if item.get("search_type", "").lower() == "city":
                return item.get("dest_id")
                
        raise HTTPException(status_code=404, detail=f"Booking.com nie znalazł miasta: {city_name}")

async def fetch_hotels(dest_id: str, checkin: str, checkout: str, adults: int):
    url = "https://booking-com15.p.rapidapi.com/api/v1/hotels/searchHotels"
    params = {
        "dest_id": dest_id, "search_type": "CITY", 
        "arrival_date": checkin, "departure_date": checkout, 
        "adults": adults, "currency_code": "PLN"
    }
    
    async with httpx.AsyncClient(timeout=20.0) as client:
        res = await client.get(url, headers=HEADERS_BOOKING, params=params)
        if res.status_code != 200: return []
        
        hotels = res.json().get("data", {}).get("hotels", [])
        
        days = (datetime.strptime(checkout, "%Y-%m-%d") - datetime.strptime(checkin, "%Y-%m-%d")).days
        if days <= 0: days = 1

        processed = []
        for hw in hotels[:10]:
            hotel = hw.get("property", {})
            price = float(hotel.get("priceBreakdown", {}).get("grossPrice", {}).get("value", 0))
            if price > 0:
                processed.append({
                    "name": hotel.get("name", "Nieznany"),
                    "total_price": price,
                    "price_per_night": round(price / days, 2)
                })
        return processed

# Endpointy

@app.post("/api/calendars")
async def get_calendars(
    origin: str = Form(..., min_length=3, max_length=3, pattern="^[A-Za-z]{3}$", description="3-literowy kod IATA, np. WAW"),
    destination: str = Form(..., min_length=3, max_length=3, pattern="^[A-Za-z]{3}$", description="3-literowy kod IATA, np. CDG")
):
    """Znajdywanie najtańszych lotów tam i z powrotem od dzisiaj."""
    today = datetime.now().strftime("%Y-%m-%d")
    
    outbound_data = await fetch_calendar(origin, destination, today)
    
    await asyncio.sleep(0.5)
    
    inbound_data = await fetch_calendar(destination, origin, today)
    
    return {
        "origin": origin,
        "destination": destination,
        "outbound_calendar": outbound_data,
        "inbound_calendar": inbound_data
    }

@app.post("/api/analyze")
async def analyze_trip(
origin: str = Form(..., min_length=3, max_length=3, pattern="^[A-Za-z]{3}$"),
    destination: str = Form(..., min_length=3, max_length=3, pattern="^[A-Za-z]{3}$"),
    departure_date: str = Form(..., pattern="^\d{4}-\d{2}-\d{2}$", description="Data w formacie YYYY-MM-DD"),
    return_date: str = Form(..., pattern="^\d{4}-\d{2}-\d{2}$", description="Data w formacie YYYY-MM-DD"),
    flight_price_total: float = Form(..., ge=0, description="Suma ceny lotu (nie może być ujemna)"),
    adults: int = Form(1, ge=1, le=10, description="Liczba dorosłych (od 1 do 10)")
):
    """Znajdywanie hoteli w wyznaczonych datach"""
    
    # CDG -> Paris -> -1456928
    city_name = await fetch_city_name_from_iata(destination)
    dest_id = await fetch_booking_dest_id(city_name)
    
    # Hotele
    hotels = await fetch_hotels(dest_id, departure_date, return_date, adults)
    
    if not hotels:
        raise HTTPException(status_code=404, detail=f"Brak wolnych hoteli w {city_name} w tym terminie.")
        
    # Szukanie najtańszego hotelu itp
    cheapest_hotel = min(hotels, key=lambda x: x["price_per_night"])
    most_expensive = max(hotels, key=lambda x: x["price_per_night"])
    avg_price = sum(h["price_per_night"] for h in hotels) / len(hotels)
    
    total_trip_cost = flight_price_total + cheapest_hotel["total_price"]

    return {
        "location_translation": {
            "iata_code": destination,
            "resolved_city_name": city_name,
            "booking_dest_id": dest_id
        },
        "trip_details": {
            "check_in": departure_date,
            "check_out": return_date,
            "flight_cost": flight_price_total,
        },
        "hotel_analysis": {
            "cheapest_hotel": cheapest_hotel,
            "most_expensive_hotel": most_expensive,
            "average_nightly_price": round(avg_price, 2)
        },
        "final_calculation": {
            "absolute_minimum_trip_cost": round(total_trip_cost, 2)
        }
    }