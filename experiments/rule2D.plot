set terminal pdf
set datafile separator ","
set style fill solid border rgb "black"
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set xlabel "m workers (2m tasks)"
set ylabel "Cmax"
set output 'rule2DCmaxGiftVsDisGiff.pdf'
plot  "data/cmax.csv" using 1:3 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:4 with lines lc "green" title 'Dis. Gift'
set output 'rule2DCmaxGiftVsLP.pdf'
plot  "data/cmax.csv" using 1:3 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:6 with lines lc "red" title 'LP'
set zlabel "Flowtime"
set output 'rule2DflowtimeGiftVsLP.pdf'
plot  "data/flowtime.csv" using 1:3 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:6 with lines lc "red" title 'LP'
set output 'rule2DflowtimeGiftVsDisGift.pdf'
plot  "data/flowtime.csv" using 1:3 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:4 with lines lc "green" title 'Dis. Gift'
