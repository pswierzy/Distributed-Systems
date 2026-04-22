# nowe głosowanie
# zobaczenie głosowania
# oddanie głosu
# zobaczenie wyników

from fastapi import FastAPI, status
from enum import Enum
from pydantic import BaseModel
from fastapi.responses import JSONResponse


class PollItem(BaseModel):
    answer: str
    vote_no: int

class Poll(BaseModel):
    id: int
    question: str
    poll_items: list[PollItem] = []

class PollCreate(BaseModel):
    question: str
    poll_items: list[str] = []

app=FastAPI( )


# TWORZENIE I PATRZENIE NA POLLS

polls: list = []
ID = 0

@app.post("/poll")
async def create_item(item: PollCreate):
    global ID
    new_poll = Poll(id=ID, question=item.question)
    ID += 1
    for answ in item.poll_items:
        new_poll.poll_items.append(PollItem(answer=answ, vote_no=0))
    polls.append(new_poll)
    return new_poll

@app.get("/poll")
async def get_polls():
    polls_quick = []
    for poll in polls:
        answs = []
        for an in poll.poll_items:
            answs.append(an.answer)
        polls_quick.append((poll.id, poll.question, answs))
    return polls_quick

@app.get("/poll/{id}")
async def get_poll(id: str):
    global ID
    if int(id) >= ID: 
        return JSONResponse(status_code=status.HTTP_404_NOT_FOUND, content="")
    return polls[int(id)]

# ODDAWANIE GŁOSU

@app.post("/vote/{id}")
async def cast_vote(id:str, vote: int):
    global ID
    if int(id) >= ID: 
        return JSONResponse(status_code=status.HTTP_404_NOT_FOUND, content="")
    poll = polls[int(id)]
    poll.poll_items[vote-1].vote_no += 1

    return poll.poll_items[vote-1].vote_no

