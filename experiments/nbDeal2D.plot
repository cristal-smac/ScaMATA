set terminal pdf
set datafile separator ","
set style fill solid border rgb "black"
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set xlabel "m workers (10m tasks)"
set ylabel "Nb of gift"
set log y
set output 'nbDeal2DCmax.pdf'
plot  "data/cmax.csv" using 1:53 with lines lc "dark-blue" title 'Gift',\
       "data/cmax.csv" using 1:58 with lines lc "dark-green" title 'Dis. Gift',\
       "data/cmax.csv" using 1:($1)*($2)/5 with lines lc "dark-cyan" title 'mn/5',\
       "data/cmax.csv" using 1:($2)*3 with lines lc "dark-red" title '3n'
#       "data/cmax.csv" using 1:54 with lines lc "dark-turquoise" title 'Propose',\
#       "data/cmax.csv" using 1:55 with lines lc "dark-cyan" title 'Accept',\
#       "data/cmax.csv" using 1:56 with lines lc "dark-red" title 'Reject',\
#       "data/cmax.csv" using 1:57 with lines lc "dark-salmon" title 'Withdraw',\
#       "data/cmax.csv" using 1:59 with lines lc "dark-magenta" title 'Inform',\
set output 'nbDeal2DFlowtime.pdf'
plot  "data/flowtime.csv" using 1:53 with lines lc "dark-blue" title 'Gift',\
       "data/flowtime.csv" using 1:58 with lines lc "dark-green" title 'Dis. Gift'
#       "data/flowtime.csv" using 1:54 with lines lc "dark-turquoise" title 'Propose',\
#       "data/flowtime.csv" using 1:55 with lines lc "dark-cyan" title 'Accept',\
#       "data/flowtime.csv" using 1:56 with lines lc "dark-red" title 'Reject',\
#       "data/flowtime.csv" using 1:57 with lines lc "dark-salmon" title 'Withdraw',\
#       "data/flowtime.csv" using 1:59 with lines lc "dark-magenta" title 'Inform',\
