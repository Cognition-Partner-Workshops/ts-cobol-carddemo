package com.carddemo.interest.batch;

/**
 * The steps interestCalcJob runs, chosen by interest.job.steps (architecture.md section 4).
 * No rule decides it: it only says how much of the job one launch assembles.
 */
enum JobSteps {

    /** calcInterestStep alone, on the adapters interest.calc.store names. */
    CALC,

    /** loadExtractsStep, calcInterestStep on the tables, exportDatasetsStep, in that order. */
    LOAD_CALC_EXPORT
}
