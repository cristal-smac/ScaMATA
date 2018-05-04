set terminal pdf
set datafile separator ","
set style fill solid border rgb "black"
set auto x
set auto y
set grid
set ticslevel 0
set style data lines
set xlabel "m workers (2m tasks)"
set ylabel "Time (s)"
set output 'time2DCmaxGiftVsDisGift.pdf'
plot  "data/cmax.csv" using 1:($7)/1E9 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:($8)/1E9 with lines lc "green" title 'Dis. Gift'
set output 'time2DCmaxGiftVsLP.pdf'
plot  "data/cmax.csv" using 1:($7)/1E9 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:($10+$11+$12)/1E9 with lines lc "red" title 'LP'
set output 'time2DFlowtimeGiftVsDisGift.pdf'
plot  "data/flowtime.csv" using 1:($7)/1E9 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:($8)/1E9 with lines lc "green" title 'Dis. Gift'
set output 'time2DFlowtimeGiftVsLP.pdf'
plot  "data/flowtime.csv" using 1:($7)/1E9 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:($10+$11+$12)/1E9 with lines lc "red" title 'LP'