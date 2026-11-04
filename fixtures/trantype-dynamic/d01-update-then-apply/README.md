# d01-update-then-apply
CBTRNDYN prepares and executes the UPDATE read from DYNSQL (dynamic SQL), then
dynamically CALLs COBTUPDT (PARMS('COBTUPDT')) which applies one add from INPFILE.
Both changes are committed together at end of task.
