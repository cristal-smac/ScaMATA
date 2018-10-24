// Copyright (C) Maxime MORGE 2018
int M = ...; // Number of workers
int N = ...; // Number of tasks
range A = 1..M; // The set of workers
range T = 1..N; // The set of tasks
range ROW= 1..M*N; // The set of rows
float Q[ROW][T] = ...; // The kcosts of the tasks for the workers

dvar float X[ROW][T] in 0..1; // The decision variables for assignements

/* Preprocessing */
float startingTime;
execute{
	var before = new Date();
	startingTime = before.getTime();
}

/* Solving the model */
minimize
	sum(i in ROW) sum(t in T) X[i][t]*Q[i][t];// the flowtime
	subject to {
		forall(t in T)
			ct_taskAssignment:
				sum(i in ROW) X[i][t] == 1.0;
		forall(r in ROW)
		  	ct_positionAssignment:
		  		sum(t in T) X[r][t] <= 1.0;
	}

/* Postprocessing */
execute{
	var endTime = new Date();
	var processingTime=endTime.getTime()-startingTime //ms
	var outputFile = new IloOplOutputFile("../../../experiments/OPL/lpOutput.txt");
	outputFile.writeln(cplex.getObjValue());//Flowtime
	outputFile.writeln(processingTime);//T in millisecond
    for(var t in thisOplModel.T){
	    for(var a in thisOplModel.A){	    
			for(var k in thisOplModel.T){
				var line = a + (k-1) * thisOplModel.M;
            	if (thisOplModel.X[line][t] == 1){
                	outputFile.writeln(a);//"ag:"+a+" t:"+t+" k:"+k+" line:"+line
            	}	
          	}
        }
     }
	outputFile.close();
}
