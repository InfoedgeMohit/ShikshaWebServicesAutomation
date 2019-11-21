package sums.info;

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

public class ValidSubs extends Common {

	static String apiPath;
	String api;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int subsId;
	String credit;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// api path
		apiPath = "sums/v1/info/validSubs";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"subsIds[]", "credit", "apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"int[]", "int"};
		// test data file
		testDataFilePath = "//src//test/resources//sums//info//ValidSubs.xlsx";
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyValidSubsApi(String subsIds, String credit, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		this.credit = credit;

		// pass api params and hit api
		if(!subsIds.equals("ignoreInput") && !credit.equals("ignoreInput"))
			apiResponse = RestAssured.given().param("subsIds[]",subsIds).param("credit",credit).when().post(api).then().extract().response();
		else if(subsIds.equals("ignoreInput") && !credit.equals("ignoreInput"))
			apiResponse = RestAssured.given().param("credit",credit).when().post(api).then().extract().response();
		else if(!subsIds.equals("ignoreInput") && credit.equals("ignoreInput"))
			apiResponse = RestAssured.given().param("subsIds[]",subsIds).when().post(api).then().extract().response();
		else if (subsIds.equals("ignoreInput") && credit.equals("ignoreInput"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){
			// In Positive cases, verify values of status, message and mapData [from Db]
			// Reporter.log("Positive case", true);
			verifyPostiveCases(subsIds, credit, jsonPath);
		} else if(statusCode == 400){
			// In Negative cases, verify values of param[field], paramType[code] and apiResponseMsg [message]
			// Reporter.log("Negative case", true);
			verifyNegativeCases(params, jsonPath, paramType, apiResponseMsgExpected);
		} else{
			Reporter.log(", InCorrect Response Code : "+statusCode, true);
			Assert.fail("InCorrect Response Code : "+statusCode);
		}

		long apiResponseActualTime = apiResponse.getTimeIn(TimeUnit.MILLISECONDS);
		responseTimes.add((int)apiResponseActualTime);

		if(apiResponseActualTime > apiResponseExpectedTime){
			Reporter.log("<br> API Response time : "+apiResponseActualTime+" MS is greater than expected time ("+apiResponseExpectedTime+" MS)", true);
			Assert.fail("API Response time : "+apiResponseActualTime+" MS is greater than expected time ("+apiResponseExpectedTime+" MS)");
		}else {
			// Reporter.log("<br> API Response time : "+apiResponseActualTime+" MS", true);
		}
	}

	public void verifyPostiveCases(String subsIds, String credit, JsonPath jsonPath){
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if(statusResponse.contentEquals("success")) {
				// Reporter.log(", Correct status [success]", true);
			} else {
				Reporter.log(", InCorrect status ["+statusResponse+"]", true);
				Assert.fail("InCorrect status ["+statusResponse+"]");
			}

			// verify message is null
			String messageResponse = jsonPath.getString("message");
			if(messageResponse == null) {
				// Reporter.log(", Correct message [null]", true);
			} else{
				Reporter.log(", InCorrect message ["+messageResponse+"]", true);
				Assert.fail("InCorrect message ["+messageResponse+"]");
			}

			String[] subsIdsArr = subsIds.split(",");
			for(int i=0; i<subsIdsArr.length; i++){
				this.subsId = Integer.parseInt(subsIdsArr[i]);
				// verify values in mapData [valid/invalid]
				// fetch details from Subscription_Product_Mapping
				ResultSet rs = exceuteDbQuery("select BaseProductId, BaseProdRemainingQuantity from Subscription_Product_Mapping where Status = 'ACTIVE' and BaseProductId in "
						+ "(496, 497, 498) and SubscriptionEndDate > now() and SubscriptionId = "+subsId+";","SUMS");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);
				
				String mapDataResponseofSingleSubsId = jsonPath.getString("mapData."+subsId+"");
				if(rowcount > 0){
					// verify value of data [valid]
					rs.next();
					int baseProductIdDb = rs.getInt("BaseProductId");
					int baseProdRemainingQuantityDb = rs.getInt("BaseProdRemainingQuantity");
					int creditInput = Integer.parseInt(credit);
					if(baseProductIdDb == 498){
						if(baseProdRemainingQuantityDb >= creditInput && mapDataResponseofSingleSubsId.equalsIgnoreCase("valid")) {
							// Reporter.log(", Correct status of "+subsId+" [valid]", true);
						} else if(baseProdRemainingQuantityDb < creditInput && mapDataResponseofSingleSubsId.equalsIgnoreCase("invalid")) {
							// Reporter.log(", Correct status of "+subsId+" [invalid]", true);
						} else{
							Reporter.log(", InCorrect status of "+subsId+" ["+mapDataResponseofSingleSubsId+"]", true);
							Assert.fail("InCorrect status of "+subsId+" ["+mapDataResponseofSingleSubsId+"]");
						}
					} else if(baseProductIdDb == 496 || baseProductIdDb == 497)
						if(mapDataResponseofSingleSubsId.equalsIgnoreCase("valid")) {
							// Reporter.log(", Correct status of "+subsId+" [valid]", true);
						} else{
							Reporter.log(", InCorrect status of "+subsId+" ["+mapDataResponseofSingleSubsId+"]", true);
							Assert.fail("InCorrect status of "+subsId+" ["+mapDataResponseofSingleSubsId+"]");
						}
				}else{
					// verify value of data [invalid]
					if(mapDataResponseofSingleSubsId.equalsIgnoreCase("invalid")) {
						// Reporter.log(", Correct status of "+subsId+" [invalid]", true);
					} else{
						Reporter.log(", InCorrect status of "+subsId+" ["+mapDataResponseofSingleSubsId+"]", true);
						Assert.fail("InCorrect status of "+subsId+" ["+mapDataResponseofSingleSubsId+"]");
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_ValidSubs(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}