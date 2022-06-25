package kp_hs;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import model.Item;




public class Reader {

	public Reader() {
	}

	@SuppressWarnings("unchecked")
	public static boolean createRandomInstance(String instanceName, int length, int min, int max) {
		boolean status = false;
		JSONArray items = new JSONArray();
		// Create the json file
		try (FileWriter file = new FileWriter("src//main//resources//instances//"+length+"_"+instanceName)) {

			// Add object until length is equal 0
			while (length > 0) {
				JSONObject item = new JSONObject();
				item.put("weight", (int)Math.floor(Math.random()*(max-min+1)+min));
				item.put("profit", (int)Math.floor(Math.random()*(max-min+1)+min));
				items.add(item);
				length--;
			}	
			file.write(items.toJSONString()); 
			file.flush();
			status = true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return status;
	}

	@SuppressWarnings("unchecked")
	public static ArrayList<Item> readKPInstance(String name) throws IOException{
		//JSON parser object to parse read file
		JSONParser jsonParser = new JSONParser();
		ArrayList<Item> items = new ArrayList<>();

		try (FileReader reader = new FileReader(name))
		{
			//Read JSON file
			Object obj = jsonParser.parse(reader);

			JSONArray instance = (JSONArray) obj;

			//Iterate over item array
			instance.forEach( (item) -> {
				items.add(parseItemObject((JSONObject) item));
			} );

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return items;
	}

	private static Item parseItemObject(JSONObject item)
	{
		return new Item((Long) item.get("weight"), (Long) item.get("profit"));
	}
}
