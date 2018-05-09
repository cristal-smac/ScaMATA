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
set output 'nbDeal2D.pdf'
plot  "data/cmax.csv" using 1:53 with lines lc "dark-blue" title 'Gift',\
       "data/cmax.csv" using 1:58 with lines lc "dark-green" title 'Dis. Gift'
#       "data/cmax.csv" using 1:54 with lines lc "dark-turquoise" title 'Propose',\
#       "data/cmax.csv" using 1:55 with lines lc "dark-cyan" title 'Accept',\
#       "data/cmax.csv" using 1:56 with lines lc "dark-red" title 'Reject',\
#       "data/cmax.csv" using 1:57 with lines lc "dark-salmon" title 'Withdraw',\
#       "data/cmax.csv" using 1:59 with lines lc "dark-magenta" title 'Inform',\