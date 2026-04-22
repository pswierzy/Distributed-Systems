
#ifndef CALC_ICE
#define CALC_ICE

module Demo
{
  enum operation { MIN, MAX, AVG };

  sequence<long> LongSeq;

  exception EmptySequenceError {
    string reason = "Sequence can't be empty";
  }

  exception NoInput {};

  struct A
  {
    short a;
    long b;
    float c;
    string d;
  }

  interface Calc
  {
    idempotent long add(int a, int b);
    idempotent long subtract(int a, int b);
    void op(A a1, short b1); //załóżmy, że to też jest operacja arytmetyczna ;)

    idempotent double avg(LongSeq seq) throws EmptySequenceError;
  };

};

#endif
