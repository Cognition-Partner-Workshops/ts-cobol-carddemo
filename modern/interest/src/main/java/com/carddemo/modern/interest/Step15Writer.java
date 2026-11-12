package com.carddemo.modern.interest;

import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

/**
 * REWRITE of ACCOUNT-FILE and WRITE of the new SYSTRAN generation, in program order. An abend keeps
 * what was written before it (CEE3ABD ends the step with STOP RUN; nothing is backed out) and stops the step.
 */
class Step15Writer implements ItemWriter<Step15Output> {

    private final InterestRepository repository;
    private final Step15Abend abend;

    Step15Writer(InterestRepository repository, Step15Abend abend) {
        this.repository = repository;
        this.abend = abend;
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
            if (out.abend()) {
                abend.set();
                StepSynchronizationManager.getContext().getStepExecution().setTerminateOnly();
            }
        }
    }
}
