#ifndef COUNT_ICE
#define COUNT_ICE
module agh {
    module Demo {
        interface Counter {
            void increment();
            void reset();
            idempotent int getValue();
        };
    };
};

#endif