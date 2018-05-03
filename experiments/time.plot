set terminal pdf
set datafile separator ","
set style fill solid border rgb "black"
set auto x
set auto y
set auto z
set grid
set hidden3d
set dgrid3d 50,50 qnorm 2
set ticslevel 0
set style data lines
set xlabel "Number of workerss"
set ylabel "Number of tasks"
set zlabel "Time (s)"
set output 'timeCmaxGiftVsDisGift.pdf'
splot  "data/cmax.csv" using 1:2:($7)/1e9 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:2:($8)/1e9 with lines lc "green" title 'Dis. Gift'
set output 'timeCmaxGiftVsLP.pdf'
splot  "data/cmax.csv" using 1:2:($7)/1e9 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:2:($9+$10+$11)/1e9 with lines lc "red" title 'LP'
set output 'timeFlowtimeGiftVsDisGift.pdf'
splot  "data/flowtime.csv" using 1:2:($7)/1e9 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:2:($8)/1e9 with lines lc "green" title 'Dis. Gift'
set output 'timeFlowtimeGiftVsLP.pdf'
splot  "data/flowtime.csv" using 1:2:($7)/1e9 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:2:($9+$10+$11)/1e9 with lines lc "red" title 'LP'