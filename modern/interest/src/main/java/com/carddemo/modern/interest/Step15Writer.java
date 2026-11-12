package com.carddemo.modern.interest;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/** REWRITE of ACCOUNT-FILE and WRITE of the new SYSTRAN generation, in program order. */
class Step15Writer implements ItemWriter<Step15Output> {

    private final InterestRepository repository;

    Step15Writer(InterestRepository repository) {
        this.repository = repository;
    }

    @Override
    public void write(Chunk<? extends Step15Output> chunk) {
        for (Step15Output out : chunk) {
            if (out.rewrite() != null) {
                repository.rewrite(out.rewrite());
            }
            if (out.transaction() != null) {
                repository.write(out.transactionSeq(), out.transaction());
            }
        }
    }
}
