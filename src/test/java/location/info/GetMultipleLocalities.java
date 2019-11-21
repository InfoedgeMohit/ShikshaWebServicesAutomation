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
public class GetMultipleLocalities extends Common {

	static String apiPath;
	String api;
	static String[] params;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int localityId;
	ExtentReports report;
	ExtentTest parent,child1;
	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
//		apiResponseExpectedTime = 200; // in MS
		loadPropertiesFromConfig("location");
		// api path
		apiPath = "location/api/v1/info/getMultipleLocalities";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"localityIds", "apiResponseMsgExpected"};
		// test data file
		testDataFilePath = "//src//test//resources//location//info//GetMultipleLocalities.xlsx";
		report =  createExtinctReport("GetMultipleLocalities");
		 parent = createParent(report, "GetMultipleLocalities","");
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetMultipleLocalitiesApi(String localityIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		child1 = createChild(report, localityIds, "");
		// pass api params and hit api
		if (localityIds.equals("ignoreHeader"))
			return;
		//apiResponse = RestAssured.given().when().post(api).then().extract().response();
		else if(localityIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST","INFOEDGE_SHIKSHA").when().post(api).then().extract().response();
		else if (!localityIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().header("AUTHREQUEST","INFOEDGE_SHIKSHA").param("localityIds[]",localityIds).when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){ // Positive case
			Reporter.log("Positive case", true);
			verifyPostiveCases(localityIds, jsonPath, report, parent, child1);
		}else if(statusCode == 400){ // Negative case
			Reporter.log("Negative case", true);
			 verifyNegativeCases(params, jsonPath, null, apiResponseMsgExpected, report, parent, child1);
		}else if(statusCode == 403){ //unauthorized request
			Reporter.log("Unauthorized request case", true);
			 verifyUnauthorizedRequestCase(jsonPath, report, parent, child1);
		}else{fail(child1,"InCorrect Response Code : " + statusCode);
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

	public void verifyPostiveCases(String localityIds, JsonPath jsonPath,ExtentReports report, ExtentTest parent, ExtentTest child){
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if(statusResponse.contentEquals("success")) {
				pass(child, "Correct status [success]");
				Reporter.log(", Correct status [success]", true);
			} else {fail(child,"InCorrect status [" + statusResponse + "]");
				Reporter.log(", InCorrect status ["+statusResponse+"]", true);
				Assert.fail("InCorrect status ["+statusResponse+"]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if(messageResponse == null) {pass(child,"Correct message [null]");
				Reporter.log(", Correct message [null]", true);
			} else{fail(child,"InCorrect message [" + messageResponse + "]");
				Reporter.log(", InCorrect message ["+messageResponse+"]", true);
				Assert.fail("InCorrect message ["+messageResponse+"]");
			}

			// loop for multiple localityIds in array
			String[] localityIdsArr = localityIds.split(",");
			for(int i=0; i<localityIdsArr.length; i++){
				this.localityId = Integer.parseInt(localityIdsArr[i]);

				// fetch data from api response [jsonPath]
				String localityNameOfSingleLocalityInDataResponse = jsonPath.getString("data."+localityId+".localityName");

				// fetch localityName from localityCityMapping
				ResultSet rs = exceuteDbQuery("SELECT lcm.localityId, lcm.localityName, lcm.zoneId, lcm.cityId, cct.state_id, cct.countryId FROM localityCityMapping lcm, "
						+ "countryCityTable cct where lcm.cityId = cct.city_id and lcm.status = 'live' and lcm.localityId = "+localityId+";","shiksha");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);

				if(rowcount > 0){

					rs.next();

					// verify localityId in data[response] as same in db
					int localityIdOfSingleLocalityInDataResponse = jsonPath.getInt("data."+localityId+".localityId");
					if(localityIdOfSingleLocalityInDataResponse == localityId) {
						pass(child, "Correct value of localityId ["+localityIdOfSingleLocalityInDataResponse+"] for localityId : "+localityId+"");
						Reporter.log(", Correct value of localityId ["+localityIdOfSingleLocalityInDataResponse+"] for localityId : "+localityId+"", true);
					} else{
						fail(child,"InCorrect value of localityId for localityId : "+localityId+" ["+localityIdOfSingleLocalityInDataResponse+"]");
						Reporter.log(", InCorrect value of localityId for localityId: "+localityId+" ["+localityIdOfSingleLocalityInDataResponse+"]", true);
						Assert.fail("InCorrect value of localityId for localityId : "+localityId+" ["+localityIdOfSingleLocalityInDataResponse+"]");
					}

					// verify locality_name in data[response] as same in db
					String localityNameDb = rs.getString("localityName");
					if(localityNameOfSingleLocalityInDataResponse.equalsIgnoreCase(localityNameDb)) {
						pass(child, " Correct name ["+localityNameOfSingleLocalityInDataResponse+"] of localityId : "+localityId+"");
						Reporter.log(", Correct name ["+localityNameOfSingleLocalityInDataResponse+"] of localityId : "+localityId+"", true);
					} else{
						
						fail(child,"InCorrect name of localityId : "+localityId+" ["+localityNameOfSingleLocalityInDataResponse+"]");
						Reporter.log(", InCorrect name of localityId: "+localityId+" ["+localityNameOfSingleLocalityInDataResponse+"]", true);
						Assert.fail("InCorrect name of localityId : "+localityId+" ["+localityNameOfSingleLocalityInDataResponse+"]");
					}

					// verify zoneId in data[response] as same in db
					int zoneIdOfSingleLocalityInDataResponse = jsonPath.getInt("data."+localityId+".zoneId");
					int zoneIdOfSingleLocalityInDb = rs.getInt("zoneId");
					if(zoneIdOfSingleLocalityInDataResponse == zoneIdOfSingleLocalityInDb) {
						Reporter.log(", Correct value of zoneId ["+zoneIdOfSingleLocalityInDataResponse+"] for localityId : "+localityId+"", true);
					} else{
						Reporter.log(", InCorrect value of zoneId for localityId: "+localityId+" ["+zoneIdOfSingleLocalityInDataResponse+"]", true);
						Assert.fail("InCorrect value of zoneId for localityId : "+localityId+" ["+zoneIdOfSingleLocalityInDataResponse+"]");
					}

					// verify cityId in data[response] as same in db
					int cityIdOfSingleLocalityInDataResponse = jsonPath.getInt("data."+localityId+".cityId");
					int cityIdOfSingleLocalityInDb = rs.getInt("cityId");
					if(cityIdOfSingleLocalityInDataResponse == cityIdOfSingleLocalityInDb) {
						pass(child, "Correct value of cityId ["+cityIdOfSingleLocalityInDataResponse+"] for localityId : "+localityId+"");
						Reporter.log(", Correct value of cityId ["+cityIdOfSingleLocalityInDataResponse+"] for localityId : "+localityId+"", true);
					} else{
						fail(child,"InCorrect value of cityId for localityId : "+localityId+" ["+cityIdOfSingleLocalityInDataResponse+"]");
						Reporter.log(", InCorrect value of cityId for localityId: "+localityId+" ["+cityIdOfSingleLocalityInDataResponse+"]", true);
						Assert.fail("InCorrect value of cityId for localityId : "+localityId+" ["+cityIdOfSingleLocalityInDataResponse+"]");
					}

					// verify state_id in data[response] as same in db
					int stateIdOfSingleLocalityInDataResponse = jsonPath.getInt("data."+localityId+".stateId");
					int stateIdOfSingleLocalityInDb = rs.getInt("state_id");
					if(stateIdOfSingleLocalityInDataResponse == stateIdOfSingleLocalityInDb) {
						pass(child,"Correct value of stateId ["+stateIdOfSingleLocalityInDataResponse+"] for localityId : "+localityId+"");
						Reporter.log(", Correct value of stateId ["+stateIdOfSingleLocalityInDataResponse+"] for localityId : "+localityId+"", true);
					} else{
						fail(child,"InCorrect value of stateId for localityId : "+localityId+" ["+stateIdOfSingleLocalityInDataResponse+"]");
						Reporter.log(", InCorrect value of stateId for localityId: "+localityId+" ["+stateIdOfSingleLocalityInDataResponse+"]", true);
						Assert.fail("InCorrect value of stateId for localityId : "+localityId+" ["+stateIdOfSingleLocalityInDataResponse+"]");
					}

					// verify countryId in data[response] as same in db
					int countryIdOfSingleLocalityInDataResponse = jsonPath.getInt("data."+localityId+".countryId");
					int countryIdOfSingleLocalityInDb = rs.getInt("countryId");
					if(countryIdOfSingleLocalityInDataResponse == countryIdOfSingleLocalityInDb) {
						pass(child, "Correct value of countryId ["+countryIdOfSingleLocalityInDataResponse+"] for localityId : "+localityId+"");
						Reporter.log(", Correct value of countryId ["+countryIdOfSingleLocalityInDataResponse+"] for localityId : "+localityId+"", true);
					} else{
						fail(child,"InCorrect countryId of localityId : "+localityId+" ["+countryIdOfSingleLocalityInDataResponse+"]");
						Reporter.log(", InCorrect value of countryId for localityId: "+localityId+" ["+countryIdOfSingleLocalityInDataResponse+"]", true);
						Assert.fail("InCorrect countryId of localityId : "+localityId+" ["+countryIdOfSingleLocalityInDataResponse+"]");
					}

				}else{
					// if localityId doesn't exist in db, then no data would be returned for that particular localityId
					if(localityNameOfSingleLocalityInDataResponse == null) {
						pass(child, " No data returned for localityId : "+localityId+" [Correct]");
						Reporter.log(", No data returned for localityId : "+localityId+" [Correct]",true);
					} else{
						fail(child,"InCorrect name of localityId : "+localityId+" ["+localityNameOfSingleLocalityInDataResponse+"]");
						Reporter.log(", InCorrect name of localityId : "+localityId+" ["+localityNameOfSingleLocalityInDataResponse+"]", true);
						Assert.fail("InCorrect name of localityId : "+localityId+" ["+localityNameOfSingleLocalityInDataResponse+"]");
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
	public static void respnseTimeStats_GetMultipleLocalities(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}