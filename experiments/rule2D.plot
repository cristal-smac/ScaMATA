set terminal pdf
set datafile separator ","
set style fill solid border rgb "black"
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set xlabel "m workers (10m tasks)"
set ylabel "Cmax"
set output 'rule2DCmaxGiftVsDisGiff.pdf'
plot  "data/cmax.csv" using 1:5 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:10 with lines lc "green" title 'Dis. Gift'
set output 'rule2DCmaxGiftVsLP.pdf'
plot  "data/cmax.csv" using 1:5 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:20 with lines lc "red" title 'LP'
set ylabel "Flowtime"
set output 'rule2DFlowtimeGiftVsLP.pdf'
plot  "data/flowtime.csv" using 1:5 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:10 with lines lc "green" title 'Dis. Gift'
set output 'rule2DFlowtimeGiftVsLP.pdf'
plot  "data/flowtime.csv" using 1:5 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:20 with lines lc "red" title 'LP'
