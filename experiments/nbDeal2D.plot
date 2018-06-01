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
set output 'figures/nbDeal2DCmax.pdf'
plot  "data/cmax.csv" using 1:53 with lines lc "dark-blue" title 'Gift',\
       "data/cmax.csv" using 1:58 with lines lc "dark-green" title 'Dis. Gift',\
       "data/cmax.csv" using 1:($2)*4 with lines lc "dark-red" title '4n'
set output 'figures/nbDeal2DFlowtime.pdf'
plot  "data/flowtime.csv" using 1:53 with lines lc "dark-blue" title 'Gift',\
       "data/flowtime.csv" using 1:58 with lines lc "dark-green" title 'Dis. Gift'