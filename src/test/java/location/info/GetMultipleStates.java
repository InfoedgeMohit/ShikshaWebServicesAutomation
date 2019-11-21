package location.info;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import com.relevantcodes.extentreports.*;
import org.testng.Assert;
import org.testng.Reporter;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import common.Common;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

public class GetMultipleStates extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int stateId;
	ExtentReports report;
	ExtentTest parent,child1;
	
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
//		apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("location");
		// api path
		apiPath = "location/api/v1/info/getMultipleStates";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"stateIds", "apiResponseMsgExpected"};
		// test data file
		testDataFilePath = "//src//test//resources//location//info//getMultipleStates.xlsx";
		 report =  createExtinctReport("GetMultipleStates");
		 parent = createParent(report, "GetMultipleStates","");	
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetMultipleStatesApi(String stateIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		child1 = createChild(report, stateIds, "");
		// pass api params and hit api
		if (stateIds.equals("ignoreHeader"))
			return;
		//apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if(stateIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST","INFOEDGE_SHIKSHA").when().post(api).then().extract().response();
		else if (!stateIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST","INFOEDGE_SHIKSHA").param("stateIds[]",stateIds).when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){ // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(stateIds, jsonPath, report, parent, child1);
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

	public void verifyPostiveCases(String stateIds, JsonPath jsonPath, ExtentReports report, ExtentTest parent, ExtentTest child){
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if(statusResponse.contentEquals("success")) {
				pass(child, "Correct status [success]");
				Reporter.log(", Correct status [success]", true);
			} else {
				fail(child,"InCorrect status [" + statusResponse + "]");
				Reporter.log(", InCorrect status ["+statusResponse+"]", true);
				Assert.fail("InCorrect status ["+statusResponse+"]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if(messageResponse == null) {
				pass(child,"Correct message [null]");
				Reporter.log(", Correct message [null]", true);
			} else{
				fail(child,"InCorrect message [" + messageResponse + "]");
				Reporter.log(", InCorrect message ["+messageResponse+"]", true);
				Assert.fail("InCorrect message ["+messageResponse+"]");
			}

			// loop for multiple stateIds in array
			String[] stateIdsArr = stateIds.split(",");
			for(int i=0; i<stateIdsArr.length; i++){
				this.stateId = Integer.parseInt(stateIdsArr[i]);

				// fetch stateName of data from api response [jsonPath]
				String stateNameOfSingleStateInDataResponse = jsonPath.getString("data."+stateId+".stateName");

				// fetch state details from stateTable
				ResultSet rs = exceuteDbQuery("SELECT state_name, enabled, countryId, tier FROM stateTable where state_id = "+stateId+";","shiksha");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);

				if(rowcount > 0){
					
					rs.next();
					
					// verify stateId in data[response]
					int stateIdOfSingleStateInDataResponse = jsonPath.getInt("data."+stateId+".stateId");
					if(stateIdOfSingleStateInDataResponse == stateId) {
						pass(child, " Correct stateId ["+stateIdOfSingleStateInDataResponse+"] of stateId : "+stateId+"");
						Reporter.log(", Correct stateId ["+stateIdOfSingleStateInDataResponse+"] of stateId : "+stateId+"", true);
					} else{
						fail(child, "InCorrect stateId of stateId: "+stateId+" ["+stateIdOfSingleStateInDataResponse+"]");
						Reporter.log(", InCorrect stateId of stateId: "+stateId+" ["+stateIdOfSingleStateInDataResponse+"]", true);
						Assert.fail("InCorrect stateId of stateId : "+stateId+" ["+stateIdOfSingleStateInDataResponse+"]");
					}
					
					// verify state_name in data[response] as same in db
					String stateNameDb = rs.getString("state_name");
					if(stateNameOfSingleStateInDataResponse.equalsIgnoreCase(stateNameDb)) {
						pass(child, "Correct name ["+stateNameOfSingleStateInDataResponse+"] of stateId : "+stateId+"");
						Reporter.log(", Correct name ["+stateNameOfSingleStateInDataResponse+"] of stateId : "+stateId+"", true);
					} else{
						fail(child, "InCorrect name of stateId: "+stateId+" ["+stateNameOfSingleStateInDataResponse+"]");
						Reporter.log(", InCorrect name of stateId: "+stateId+" ["+stateNameOfSingleStateInDataResponse+"]", true);
						Assert.fail("InCorrect name of stateId : "+stateId+" ["+stateNameOfSingleStateInDataResponse+"]");
					}
					
					// verify enabled in data[response] as same in db
					int enabledFlagOfSingleStateInDataResponse = jsonPath.getInt("data."+stateId+".enabled");
					int enabledDb = rs.getInt("enabled");
					if(enabledFlagOfSingleStateInDataResponse == enabledDb) {
						pass(child, "Correct value of enabled ["+enabledFlagOfSingleStateInDataResponse+"] for stateId : "+stateId+"");
						Reporter.log(", Correct value of enabled ["+enabledFlagOfSingleStateInDataResponse+"] for stateId : "+stateId+"", true);
					} else{
						fail(child, "InCorrect value of enabled for stateId: "+stateId+" ["+enabledFlagOfSingleStateInDataResponse+"]");
						Reporter.log(", InCorrect value of enabled for stateId: "+stateId+" ["+enabledFlagOfSingleStateInDataResponse+"]", true);
						Assert.fail("InCorrect value of enabled for stateId : "+stateId+" ["+enabledFlagOfSingleStateInDataResponse+"]");
					}
					
					// verify countryId in data[response] as same in db
					int countryIdOfSingleStateInDataResponse = jsonPath.getInt("data."+stateId+".countryId");
					int countryIdDb = rs.getInt("countryId");
					if(countryIdOfSingleStateInDataResponse == countryIdDb) {
						pass(child, "Correct value of countryId ["+countryIdOfSingleStateInDataResponse+"] for stateId : "+stateId+"");
						Reporter.log(", Correct value of countryId ["+countryIdOfSingleStateInDataResponse+"] for stateId : "+stateId+"", true);
					} else{
						fail(child, "InCorrect value of countryId for stateId: "+stateId+" ["+countryIdOfSingleStateInDataResponse+"]");
						Reporter.log(", InCorrect value of countryId for stateId: "+stateId+" ["+countryIdOfSingleStateInDataResponse+"]", true);
						Assert.fail("InCorrect value of countryId for stateId : "+stateId+" ["+countryIdOfSingleStateInDataResponse+"]");
					}
					
					// verify tier in data[response] as same in db
					int tierOfSingleStateInDataResponse = jsonPath.getInt("data."+stateId+".tier");
					int tierDb = rs.getInt("tier");
					if(tierOfSingleStateInDataResponse == tierDb) {
						pass(child, " Correct value of tier ["+tierOfSingleStateInDataResponse+"] for stateId : "+stateId+"");
						Reporter.log(", Correct value of tier ["+tierOfSingleStateInDataResponse+"] for stateId : "+stateId+"", true);
					} else{
						fail(child,"InCorrect value of tier for stateId : "+stateId+" ["+tierOfSingleStateInDataResponse+"]");
						Reporter.log(", InCorrect value of tier for stateId: "+stateId+" ["+tierOfSingleStateInDataResponse+"]", true);
						Assert.fail("InCorrect value of tier for stateId : "+stateId+" ["+tierOfSingleStateInDataResponse+"]");
					}
					
				}else{
					// if stateId doesn't exist in db, then no data would be returned for that particular stateId
					if(stateNameOfSingleStateInDataResponse == null) {
						pass(child, "No data returned for stateId : "+stateId+" [Correct]");
						Reporter.log(", No data returned for stateId : "+stateId+" [Correct]",true);
					} else{
						fail(child,"InCorrect name of stateId : "+stateId+" ["+stateNameOfSingleStateInDataResponse+"]");
						Reporter.log(", InCorrect name of stateId : "+stateId+" ["+stateNameOfSingleStateInDataResponse+"]", true);
						Assert.fail("InCorrect name of stateId : "+stateId+" ["+stateNameOfSingleStateInDataResponse+"]");
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
	public static void respnseTimeStats_GetMultipleStates(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}