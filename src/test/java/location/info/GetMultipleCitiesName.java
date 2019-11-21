package location.info;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import com.relevantcodes.extentreports.*;
public class GetMultipleCitiesName extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int cityId;
	ExtentReports report;
	ExtentTest parent,child1;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
//		apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("location");
		// api path
		apiPath = "location/api/v1/info/getMultipleCitiesName";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"cityIds", "apiResponseMsgExpected"};
		// test data file
		testDataFilePath = "//src//test//resources//location//info//GetMultipleCitiesName.xlsx";
		 report =  createExtinctReport("GetMultipleCitiesName");
		 parent = createParent(report, "GetMultipleCitiesName","");	
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetMultipleCitiesNameApi(String cityIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		child1 = createChild(report, cityIds, "");
		// pass api params and hit api
		if (cityIds.equals("ignoreHeader"))
			return;
		//apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if(cityIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST","INFOEDGE_SHIKSHA").when().post(api).then().extract().response();
		else if (!cityIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST","INFOEDGE_SHIKSHA").param("cityIds[]",cityIds).when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){ // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(cityIds, jsonPath, report, parent, child1);
		}else if(statusCode == 400){ // Negative case
			Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected, report, parent, child1);
		}else if(statusCode == 403){ //unauthorized request
			Reporter.log("Unauthorized request case", true);
			verifyUnauthorizedRequestCase(jsonPath, report, parent, child1);
		}else{
			 fail(child1,"InCorrect Response Code : " + statusCode);
			Reporter.log(", InCorrect Response Code : "+statusCode, true);
			Assert.fail("InCorrect Response Code : "+statusCode);
		}

		long apiResponseActualTime = apiResponse.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int)apiResponseActualTime);

		if(apiResponseActualTime > apiResponseExpectedTime){
			 fail(child1,"API Response time : " + apiResponseActualTime + " MS is greater than expected time ("
						+ apiResponseExpectedTime + " MS)");
			Reporter.log("<br> API Response time : "+apiResponseActualTime+" MS is greater than expected time ("+apiResponseExpectedTime+" MS)", true);
			Assert.fail("API Response time : "+apiResponseActualTime+" MS is greater than expected time ("+apiResponseExpectedTime+" MS)");
		}else {
			pass(child1," API Response time : " + apiResponseActualTime + " MS");
			Reporter.log("<br> API Response time : "+apiResponseActualTime+" MS", true);
		}
	}

	public void verifyPostiveCases(String cityIds, JsonPath jsonPath,ExtentReports report, ExtentTest parent, ExtentTest child){
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if(statusResponse.contentEquals("success")) {pass(child, "Correct status [success]");
				Reporter.log(", Correct status [success]", true);
			} else {fail(child,"InCorrect status [" + statusResponse + "]");
				Reporter.log(", InCorrect status ["+statusResponse+"]", true);
				Assert.fail("InCorrect status ["+statusResponse+"]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if(messageResponse == null) {pass(child,"Correct message [null]");
				Reporter.log(", Correct message [null]", true);
			} else{ fail(child,"InCorrect message [" + messageResponse + "]");
				Reporter.log(", InCorrect message ["+messageResponse+"]", true);
				Assert.fail("InCorrect message ["+messageResponse+"]");
			}

			// loop for multiple cityIds in array
			String[] cityIdsArr = cityIds.split(",");
			for(int i=0; i<cityIdsArr.length; i++){
				this.cityId = Integer.parseInt(cityIdsArr[i]);

				// fetch data from api response [jsonPath]
				String cityNameOfSingleCityInDataResponse = jsonPath.getString("data."+cityId+"");

				// fetch city_name from countryCityTable
				ResultSet rs = exceuteDbQuery("SELECT city_name FROM countryCityTable where city_id = "+cityId+";","shiksha");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);

				if(rowcount > 0){
					// verify city_name in data[response] as same in db
					rs.next();
					String cityNameDb = rs.getString("city_name");
					if(cityNameOfSingleCityInDataResponse.equalsIgnoreCase(cityNameDb)) {
						pass(child, "Correct name ["+cityNameOfSingleCityInDataResponse+"] of cityId : "+cityId+"");
						Reporter.log(", Correct name ["+cityNameOfSingleCityInDataResponse+"] of cityId : "+cityId+"", true);
					} else{
						fail(child, "InCorrect name of cityId : "+cityId+" ["+cityNameOfSingleCityInDataResponse+"]");
						Reporter.log(", InCorrect name of cityId: "+cityId+" ["+cityNameOfSingleCityInDataResponse+"]", true);
						Assert.fail("InCorrect name of cityId : "+cityId+" ["+cityNameOfSingleCityInDataResponse+"]");
					}
				}else{
					// if cityId doesn't exist in db, then no data would be returned for that particular cityId
					if(cityNameOfSingleCityInDataResponse == null) {
						pass(child, "No data returned for cityId : "+cityId+" [Correct]");
						Reporter.log(", No data returned for cityId : "+cityId+" [Correct]",true);
					} else{
						fail(child, "InCorrect name of cityId : "+cityId+" ["+cityNameOfSingleCityInDataResponse+"]");
						Reporter.log(", InCorrect name of cityId : "+cityId+" ["+cityNameOfSingleCityInDataResponse+"]", true);
						Assert.fail("InCorrect name of cityId : "+cityId+" ["+cityNameOfSingleCityInDataResponse+"]");
					}
				}
			}
		} catch (SQLException e) {
			fail(child, e.toString());
			e.printStackTrace();
			Assert.fail("SQLException occured :" + e.getMessage());
		}
		finally{
			closeChild(parent, child, report);
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_GetMultipleCitiesName(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}