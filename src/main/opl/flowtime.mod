// Copyright (C) Maxime MORGE 2018
int M = ...; // Number of agents
int N = ...; // Number of task
range A = 1..M; // The set of agents
range T = 1..N; // The set of tasks
float C[A][T] = ...; // The costs of the tasks for the agents

dvar int X[A][T] in 0..1; // The decision variables

/* Preprocessing */
float startingTime;
execute{
	var before = new Date();
	startingTime = before.getTime();
}

/* Solving the model */
minimize
	sum(i in A) sum(t in T) X[i][t]*C[i][t];// the flowtime
	subject to {
		forall(t in T)
			ct_taskAssignment:
				sum(i in A) X[i][t] == 1.0;
	}

/* Postprocessing */
execute{
	var endTime = new Date();
	var processingTime=endTime.getTime()-startingTime //ms
	var outputFile = new IloOplOutputFile("../../../experiments/OPL/lpOutput.txt");
	outputFile.writeln(cplex.getObjValue());//Flowtime
	outputFile.writeln(processingTime);//T in millisecond
    for(a in thisOplModel.A){
        for(t in thisOplModel.T){
            if (thisOplModel.X[a][t] == 1){
                outputFile.writeln(a);
            }
        }
     }
	outputFile.close();
}
