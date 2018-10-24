set terminal pdfcairo
set datafile separator ","
set style fill solid border rgb "black"
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set key above left
set xlabel "m workers (5m tasks)"
set ylabel "Number of deals"
set log y
set output 'figures/nbDeal2DCmax.pdf'
plot  "data/cmax.csv" using 1:71 with lines dt 1 lc "dark-blue" title 'nb. of gifts for the strategy Gift',\
       "data/cmax.csv" using 1:79 with lines dt 3 lc "dark-green" title 'nb. of gifts for the strategy Swap',\
       "data/cmax.csv" using 1:80 with lines dt 5 lc "dark-red" title 'nb. of swaps for the strategy Swap'
set output 'figures/nbDeal2DFlowtime.pdf'
plot  "data/flowtime.csv" using 1:63 with lines dt 7 lc "light-blue" title 'nb. of gifts for the strategy Gift (LF)',\
      "data/flowtime.csv" using 1:64 with lines dt 9 lc "light-green" title 'nb. of gifts for the strategy Swap (LF)',\
      "data/flowtime.csv" using 1:65 with lines dt 11 lc "light-red" title 'nb. of swaps for the strategy Swap (LF)'
#     "data/flowtime.csv" using 1:71 with lines dt 1 lc "dark-blue" title 'nb. of gifts for the strategy Gift (LC)',\
#     "data/flowtime.csv" using 1:79 with lines dt 3 lc "dark-green" title 'nb. of gifts for the strategy Swap (LC)',\
#     "data/flowtime.csv" using 1:80 with lines dt 5 lc "dark-red" title 'nb. of swaps for the strategy Swap (LC)',\

