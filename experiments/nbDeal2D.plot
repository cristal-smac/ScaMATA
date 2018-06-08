set terminal pdf
set datafile separator ","
set style fill solid border rgb "black"
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set key bottom right
set xlabel "m workers (5m tasks)"
set ylabel "Number of deals"
set log y
set output 'figures/nbDeal2DCmax.pdf'
plot  "data/cmax.csv" using 1:63 with lines dt 1 lc "dark-blue" title 'nb. of gifts for the strategy Gift',\
       "data/cmax.csv" using 1:64 with lines dt 3 lc "dark-green" title 'nb. of gifts for the strategy Swap',\
       "data/cmax.csv" using 1:65 with lines dt 5 lc "dark-red" title 'nb. of swaps for the strategy Swap'
set output 'figures/nbDeal2DFlowtime.pdf'
set xlabel "m workers (10m tasks)"
plot  "data/flowtime.csv" using 1:63 with lines dt 1 lc "dark-blue" title 'nb. of gifts  for the strategy Gift',\
       "data/flowtime.csv" using 1:64 with lines dt 3 lc "dark-green" title 'nb. of gifts for the strategy Swap',\
       "data/flowtime.csv" using 1:65 with lines dt 5 lc "dark-red" title 'nb. of swaps for the strategy Swap'
