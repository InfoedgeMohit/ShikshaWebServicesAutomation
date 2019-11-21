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

public class GetSubsPortingType extends Common {

	static String apiPath;
	String api;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	int subsId;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// api path
		apiPath = "sums/v1/info/getSubsPortingType";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"subsIds[]", "apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"int[]"};
		// test data file
		testDataFilePath = "//src//test/resources//sums//info//GetSubsPortingType.xlsx";
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetSubsPortingTypeApi(String subsIds, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;

		// pass api params and hit api
		if(!subsIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().param("subsIds[]",subsIds).when().post(api).then().extract().response();
		else if (subsIds.equals("ignoreInput"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){
			// In Positive cases, verify values of status, message and mapData [from Db]
			// Reporter.log("Positive case", true);
			verifyPostiveCases(subsIds, jsonPath);
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

	public void verifyPostiveCases(String subsIds, JsonPath jsonPath){
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

			// loop for multiple subsIds in array
			String[] subsIdsArr = subsIds.split(",");
			for(int i=0; i<subsIdsArr.length; i++){
				this.subsId = Integer.parseInt(subsIdsArr[i]);

				// fetch mapData from api response [jsonPath]
				String mapDataResponseofSingleSubsId = jsonPath.getString("mapData."+subsId+"");

				// fetch BaseProductId from Subscription_Product_Mapping
				ResultSet rs = exceuteDbQuery("select BaseProductId from Subscription_Product_Mapping where SubscriptionId = "+subsId+";","SUMS");

				// get row count of resultSet of sql query
				int rowcount = getCountOfResultSet(rs);

				if(rowcount > 0){
					// verify type of subsId in mapData as lead_quantity/lead_duration/response_duration
					rs.next();
					int baseProductIdDb = rs.getInt("BaseProductId");
					// 'lead_quantity' if BaseProductId  = 498
					if(baseProductIdDb == 498 && mapDataResponseofSingleSubsId.equalsIgnoreCase("lead_quantity")) {
						// Reporter.log(", Correct type of subsId : "+subsId+" [lead_quantity]", true);
					// 'lead_duration' if BaseProductId  = 497
					} else if(baseProductIdDb == 497 && mapDataResponseofSingleSubsId.equalsIgnoreCase("lead_duration")) {
						// Reporter.log(", Correct type of subsId : "+subsId+" [lead_duration]", true);
					// 'response_duration' if BaseProductId  = 496
					} else if(baseProductIdDb == 496 && mapDataResponseofSingleSubsId.equalsIgnoreCase("response_duration")) {
						// Reporter.log(", Correct type of subsId : "+subsId+" [response_duration]", true);
					// Default 'lead_quantity' if BaseProductId not in (496, 497, 498)
					} else if(mapDataResponseofSingleSubsId.equalsIgnoreCase("lead_quantity")) {
						// Reporter.log(", Correct type of subsId : "+subsId+" [default : lead_quantity]", true);
					} else{
						Reporter.log(", InCorrect type of subsId : "+subsId+" ["+mapDataResponseofSingleSubsId+"]", true);
						Assert.fail("InCorrect type of subsId : "+subsId+" ["+mapDataResponseofSingleSubsId+"]");
					}
				}else{
					// if subsId doesn't exist in db, then no data would be returned for that particular subsId
					if(mapDataResponseofSingleSubsId == null) {
						// Reporter.log(", No data returned for "+subsId+" [Correct]",true);
					} else{
						Reporter.log(", InCorrect type of "+subsId+" ["+mapDataResponseofSingleSubsId+"]", true);
						Assert.fail("InCorrect type of "+subsId+" ["+mapDataResponseofSingleSubsId+"]");
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_GetSubsPortingType(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}