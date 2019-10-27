/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kp_hs;

import algorithms.DiscreteBinaryHarmonySearch;
import algorithms.HarmonySearch;
import algorithms.HillClimbingFirstAscent;
import algorithms.ImprovedHarmonySearch;
import classes.Item;
import classes.Knapsack;
import classes.Model;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

/**
 *
 * @author dmorfav
 */
public class KP_HS {

    void createKPInstances(int instanceSize,String FileName){
        List<Item> items = new ArrayList<Item>();
        for(int i=0; i<instanceSize; i++){
            Item it = new Item((int)(Math.random()* 30), (int)(Math.random()* 100));
            items.add(it);
        }
        
        Workbook wb = new HSSFWorkbook(); 
        HSSFSheet sheet = (HSSFSheet) wb.createSheet("Items for Knapsack"); 
        //HSSFRow rowhead = sheet.createRow((short) 2); 
        HSSFRow rowheadmain = sheet.createRow(0);
        rowheadmain.createCell(0).setCellValue("WEIGHT");
        rowheadmain.createCell(1).setCellValue("PROFIT");        
        
        for(int i=1; i<instanceSize; i++){
            HSSFRow rowhead = sheet.createRow(i);
            rowhead.createCell(0).setCellValue(items.get(i).getWeight());
            rowhead.createCell(1).setCellValue(items.get(i).getProfit());
        }           
        
        try{
            String filepath = "src//main//java//instances//"+FileName+".xls";
                FileOutputStream fileOut = new FileOutputStream(filepath);
                wb.write(fileOut);
                fileOut.flush();
        }catch(IOException e){
            System.out.println(e.getMessage());
        }          
    }
    
    static List<Item> readKPInstance(int selection) throws IOException{
        List<Item> items = new ArrayList<Item>();
        switch(selection){
            case 1: 
                //Instance of 5 Items
                try{
                    FileInputStream ficheroXlsx = new FileInputStream(new File("src//main//java//instances//Instance01.xls"));
                    Workbook ficheroWb = new HSSFWorkbook(ficheroXlsx);
                    Sheet sheet = ficheroWb.getSheetAt(0);
                    for(int i=1; i<6; i++){
                        Row row = sheet.getRow(i);
                        Item it = new Item((int)row.getCell(1).getNumericCellValue(), (int)row.getCell(0).getNumericCellValue());
                        items.add(it);
                    }
                }catch (IOException ex) {
                    throw new IOException("File is corrupt or doesn't exist");
                }                 
                break;
            case 2: 
                //Instance of 15 Items
                try{
                    FileInputStream ficheroXlsx = new FileInputStream(new File("src//main//java//instances//Instance02.xls"));
                    Workbook ficheroWb = new HSSFWorkbook(ficheroXlsx);
                    Sheet sheet = ficheroWb.getSheetAt(0);
                    for(int i=1; i<16; i++){
                        Row row = sheet.getRow(i);
                        Item it = new Item((int)row.getCell(1).getNumericCellValue(), (int)row.getCell(0).getNumericCellValue());
                        items.add(it);
                    }
                }catch (IOException ex) {
                    throw new IOException("File is corrupt or doesn't exist");
                }
                break;                
            case 3: 
                //Instance of 50 Items
                try{
                    FileInputStream ficheroXlsx = new FileInputStream(new File("src//main//java//instances//Instance03.xls"));
                    Workbook ficheroWb = new HSSFWorkbook(ficheroXlsx);
                    Sheet sheet = ficheroWb.getSheetAt(0);
                    for(int i=1; i<51; i++){
                        Row row = sheet.getRow(i);
                        Item it = new Item((int)row.getCell(1).getNumericCellValue(), (int)row.getCell(0).getNumericCellValue());
                        items.add(it);
                    }
                }catch (IOException ex) {
                    throw new IOException("File is corrupt or doesn't exist");
                }
                break;                 
            case 4: 
                //Instance of 150 Items
                try{
                    FileInputStream ficheroXlsx = new FileInputStream(new File("src//main//java//instances//Instance04.xls"));
                    Workbook ficheroWb = new HSSFWorkbook(ficheroXlsx);
                    Sheet sheet = ficheroWb.getSheetAt(0);
                    for(int i=1; i<151; i++){
                        Row row = sheet.getRow(i);
                        Item it = new Item((int)row.getCell(1).getNumericCellValue(), (int)row.getCell(0).getNumericCellValue());
                        items.add(it);
                    }
                }catch (IOException ex) {
                    throw new IOException("File is corrupt or doesn't exist");
                }
                break;  
            case 5: 
                //Instance of 10 Items
                try{
                    FileInputStream ficheroXlsx = new FileInputStream(new File("src//main//java//instances//F1.xls"));
                    Workbook ficheroWb = new HSSFWorkbook(ficheroXlsx);
                    Sheet sheet = ficheroWb.getSheetAt(0);
                    for(int i=1; i<11; i++){
                        Row row = sheet.getRow(i);
                        Item it = new Item((int)row.getCell(1).getNumericCellValue(), (int)row.getCell(0).getNumericCellValue());
                        items.add(it);
                    }
                }catch (IOException ex) {
                    throw new IOException("File is corrupt or doesn't exist");
                }
                break;  
            case 6: 
                //Instance of 15 Items
                try{
                    FileInputStream ficheroXlsx = new FileInputStream(new File("src//main//java//instances//PC07.xls"));
                    Workbook ficheroWb = new HSSFWorkbook(ficheroXlsx);
                    Sheet sheet = ficheroWb.getSheetAt(0);
                    for(int i=1; i<16; i++){
                        Row row = sheet.getRow(i);
                        Item it = new Item((int)row.getCell(1).getNumericCellValue(), (int)row.getCell(0).getNumericCellValue());
                        items.add(it);
                    }
                }catch (IOException ex) {
                    throw new IOException("File is corrupt or doesn't exist");
                }
                break;                 
        }
        return items;
    }
    public static void executeHarmonySearch(int instanceSize, Model m){
        //Reference
//        System.out.print("REFERENCE");
//        int[] tmp = {1,0,1,0,1,0,1,1,1,0,0,0,0,1,1};
//        System.out.println(m.evaluateOF(tmp)); 
        //**********************************************//*
        HarmonySearch hs = new HarmonySearch(instanceSize, m);
        long startTimeHS = System.currentTimeMillis();
        int[] result = hs.HarmonySearchAlgorithm();
        long currentTimeHS = System.currentTimeMillis(); 
        long elapsedTimehs = (currentTimeHS-startTimeHS);
        int counter = 0;
        for(int j=0; j<instanceSize; j++){
            if((int)result[j] == 1){
                counter++;
            } 
            System.out.print((int)result[j]);
        }
        System.out.println("");
        System.out.print("HARMONY SEARCH");
        System.out.println("");        
        System.out.print("Entraron "+counter+" elementos en la mochila");
        System.out.println("");
        System.out.print("Beneficio máximo: ");
        System.out.print(m.evaluateOF(result));
        System.out.println("");
        System.out.print("Peso máximo: ");
        System.out.print(m.totalWeight(result));
        System.out.println("");        
        System.out.print("Tiempo de ejecución: ");
        //System.out.print((elapsedTimehs/1000)*60+" segundos");
        System.out.print(elapsedTimehs+" milisegundos");
        System.out.println("");        
    }
    
    public static void executeImprovedHarmonySearch(int instanceSize, Model m){
        //Reference
//        System.out.println("REFERENCE");
//        int[] tmp = {1,0,1,0,1,0,1,1,1,0,0,0,0,1,1};
//        System.out.println(m.evaluateOF(tmp)); 
        //**********************************************//*
        //Improved Harmony Search        
        ImprovedHarmonySearch ihs = new ImprovedHarmonySearch(instanceSize, m);
        long startTimeIHS = System.currentTimeMillis();
        int[] resultIHS = ihs.HarmonySearchAlgorithm();
        long currentTimeIHS = System.currentTimeMillis(); 
        long elapsedTimeIHS = (currentTimeIHS-startTimeIHS);         
        int counter2 = 0;
        for(int l=0; l<instanceSize; l++){
            if((int)resultIHS[l] == 1){
                counter2++;
            } 
            System.out.print(resultIHS[l]);
        }
        System.out.println("");
        System.out.print("IMPROVED HARMONY SEARCH");
        System.out.println("");        
        System.out.print("Entraron "+counter2+" elementos en la mochila");
        System.out.println("");
        System.out.print("Beneficio máximo: ");
        System.out.print(m.evaluateOF(resultIHS));
        System.out.println("");
        System.out.print("Peso máximo: ");
        System.out.print(m.totalWeight(resultIHS));
        System.out.println("");        
        System.out.print("Tiempo de ejecución: ");
        //System.out.print((elapsedTimehs/1000)*60+" segundos");
        System.out.print(elapsedTimeIHS+" milisegundos");
        System.out.println("");        
    }
    
    public static void executeDiscreteBinaryHarmonySearch(int instanceSize, Model m){
        //Reference
//        System.out.print("REFERENCE: ");
//        int[] tmp = {1,0,1,0,1,0,1,1,1,0,0,0,0,1,1};
//        System.out.println(m.evaluateOF(tmp)); 
        /********************************************/
        DiscreteBinaryHarmonySearch dbhs = new DiscreteBinaryHarmonySearch(instanceSize, m);
        long startTimeHS = System.currentTimeMillis();
        int[] result = dbhs.HarmonySearchAlgorithm();
        long currentTimeHS = System.currentTimeMillis(); 
        long elapsedTimehs = (currentTimeHS-startTimeHS);
        int counter = 0;
        for(int j=0; j<instanceSize; j++){
            if((int)result[j] == 1){
                counter++;
            }  
            System.out.print(result[j]);
        }
        System.out.println("");
        System.out.print("DISCRETE BINARY HARMONY SEARCH");
        System.out.println("");        
        System.out.print("Entraron "+counter+" elementos en la mochila");
        System.out.println("");
        System.out.print("Beneficio máximo: ");
        System.out.print(m.evaluateOF(result));
        System.out.println("");
        System.out.print("Peso máximo: ");
        System.out.print(m.totalWeight(result));
        System.out.println("");        
        System.out.print("Tiempo de ejecución: ");
        //System.out.print((elapsedTimehs/1000)*60+" segundos");
        System.out.print(elapsedTimehs+" milisegundos");
        System.out.println("");        
    }
    
    public static void executeHillClimbingFAJI(int instanceSize, Model m){
        //temporary
//        int[] tmp = {1,0,1,0,1,0,1,1,1,0,0,0,0,1,1};
//        System.out.println(m.evaluateOF(tmp));        
        //Hill Climbing
        HillClimbingFirstAscent hc = new HillClimbingFirstAscent(instanceSize, m);
        long startTimeHC = System.currentTimeMillis();
        int[] resultHC = hc.climbTheHill();
        long currentTimeHC = System.currentTimeMillis(); 
        long elapsedTimeHC = (currentTimeHC-startTimeHC);
        //int[] resultHC = new int[instanceSize];
        //System.arraycopy(hc.getGlobal(), 0, resultHC, 0, instanceSize);
        int counter3 = 0;
        for(int k=0; k<instanceSize; k++){
            if(resultHC[k] == 1){
                counter3++;
            }
            System.out.print(resultHC[k]);
        }
        System.out.println("");
        System.out.print("HILL CLIMBING FIRST ASCENT ACCEPTING EQUALS");
        System.out.println("");        
        System.out.print("Entraron "+counter3+" elementos en la mochila");
        System.out.println("");
        System.out.print("Beneficio máximo: ");
        System.out.print(m.evaluateOF(resultHC));
        System.out.println("");
        System.out.print("Peso máximo: ");
        System.out.print(m.totalWeight(resultHC));
        System.out.println("");        
        System.out.print("Tiempo de ejecución: ");
        //System.out.print((elapsedTimeHC/1000)*60+" segundos");
        System.out.print(elapsedTimeHC+" milisegundos");
        System.out.println("");
                
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try{
           /* KP_HS instance = new KP_HS();
            instance.createKPInstances(25,"Prueba");*/
            //Para instancia 5, KP = 269 y ProblemSize = 11
            List<Item> items = readKPInstance(3);
            Knapsack kp = new Knapsack(354);
            Model m = new Model(items, kp);
            //executeHarmonySearch(51, m);
            //executeDiscreteBinaryHarmonySearch(51, m);
            //executeImprovedHarmonySearch(51, m);
            executeHillClimbingFAJI(51, m);
        }catch (IOException ex) {
           System.out.println(ex.getMessage());
        }
    }
    
}
