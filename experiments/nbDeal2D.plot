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
plot  "data/cmax.csv" using 1:63 with lines lc "dark-blue" title 'nbGift 4 GiftOnly',\
       "data/cmax.csv" using 1:64 with lines lc "dark-green" title 'nbGift 4 SwapAndGift',\
       "data/cmax.csv" using 1:65 with lines lc "dark-red" title 'nbSwap 4 SwapAndGift'
set output 'figures/nbDeal2DFlowtime.pdf'
plot  "data/flowtime.csv" using 1:63 with lines lc "dark-blue" title 'nbGift 4 GiftOnly',\
       "data/flowtime.csv" using 1:64 with lines lc "dark-green" title 'nbGift 4 SwapAndGift',\
       "data/flowtime.csv" using 1:65 with lines lc "dark-red" title 'nbSwap 4 SwapAndGift'
