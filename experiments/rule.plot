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
set xlabel "Number of agents"
set ylabel "Number of tasks"
set zlabel "Cmax"
set output 'ruleCmaxGiftVsDisGiff.pdf'
splot  "data/cmax.csv" using 1:2:3 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:2:4 with lines lc "green" title 'Dis. Gift'
set output 'ruleCmaxGiftVsLP.pdf'
splot  "data/cmax.csv" using 1:2:3 with lines lc "blue" title 'Gift',\
       "data/cmax.csv" using 1:2:6 with lines lc "red" title 'LP'
set zlabel "Flowtime"
set output 'ruleflowtimeGiftVsLP.pdf'
splot  "data/flowtime.csv" using 1:2:3 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:2:6 with lines lc "red" title 'LP'
set output 'ruleflowtimeGiftVsDisGift.pdf'
splot  "data/flowtime.csv" using 1:2:3 with lines lc "blue" title 'Gift',\
       "data/flowtime.csv" using 1:2:4 with lines lc "green" title 'Dis. Gift'