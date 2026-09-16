package kp_hs;

import model.Knapsack;

public class Summary {
	public static void print(String algorithmName,int[] result, Knapsack kp, long startTime, long finishTime){
		try {
	        int counter = 0;
	        for(int i=0; i<kp.instanceSize(); i++){
	            if((int)result[i] == 1){
	                counter++;
	            } 
	            System.out.print(result[i]);
	        }
	        System.out.println("");
	        System.out.print(algorithmName);
	        System.out.println("");        
	        System.out.print("Entraron "+counter+" elementos en la mochila");
	        System.out.println("");
	        System.out.print("Beneficio máximo: ");
	        System.out.print(kp.evaluateOF(result));
	        System.out.println("");
	        System.out.print("Peso máximo: ");
	        System.out.print(kp.totalWeight(result));
	        System.out.println("");        
	        System.out.print("Tiempo de ejecución: ");
	        System.out.print(finishTime-startTime+" milisegundos");
	        System.out.println("");        
		}catch (Exception e) {
			e.printStackTrace();
		}
	}
}
