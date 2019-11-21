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

public class CollegeLinkSubsDetails extends Common {

	static String apiPath;
	String api;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	String subsId;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// api path
		apiPath = "sums/v1/info/collegeLinkSubsDetails";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"subsId", "apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"int"};
		// test data file
		testDataFilePath = "//src//test/resources//sums//info//CollegeLinkSubsDetails.xlsx";
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyCollegeLinkSubsDetailsApi(String subsId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		this.subsId = subsId;

		// pass api params and hit api
		if(!subsId.equals("ignoreInput"))
			apiResponse = RestAssured.given().param("subsId",subsId).when().post(api).then().extract().response();
		else if (subsId.equals("ignoreInput"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();
		
		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){
			// In Positive cases, verify values of status, message and mapData[from Db]
			// Reporter.log("Positive case", true);
			verifyPostiveCases(subsId, jsonPath);
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

	public void verifyPostiveCases(String subsId, JsonPath jsonPath){
		try {
			// verify status is success
			String statusResponse = jsonPath.getString("status");
			if(statusResponse.contentEquals("success")) {
				// Reporter.log(", Correct status [success]", true);
			}
			else {
				Reporter.log(", InCorrect status ["+statusResponse+"]", true);
				Assert.fail("InCorrect status ["+statusResponse+"]");
			}

			// fetch details from Subscription and Base_Prod_Property_Mapping
			ResultSet rs = exceuteDbQuery("select bp.BaseProductId, bp.BasePropertyId, bp.BasePropertyValue from Subscription s, Base_Prod_Property_Mapping bp where "
					+ "s.BaseProductId = bp.BaseProductId and s.SubscriptionId = "+subsId+";","SUMS");

			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);

			if(rowcount > 0){
				// verify message is null
				String messageResponse = jsonPath.getString("message");
				if(messageResponse == null) {
					// Reporter.log(", Correct message [null]", true);
				}
				else{
					Reporter.log(", InCorrect message ["+messageResponse+"]", true);
					Assert.fail("InCorrect message ["+messageResponse+"]");
				}

				// verify details in listData
				int i=0;
				while(rs.next() && i<rowcount){
					// Reporter.log("<br> Veriify data of listData["+i+"] >>", true);

					// check value of BaseProductId
					int baseProductDb = rs.getInt("BaseProductId");
					int baseProductResponse = jsonPath.getInt("listData["+i+"].BaseProductId");
					if(baseProductResponse == baseProductDb) {
						// Reporter.log(", Correct value of BaseProductId ["+baseProductDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of BaseProductId ["+baseProductDb+"]", true);
						Assert.fail("InCorrect value of BaseProductId ["+baseProductDb+"]");
					}
					
					// check value of BasePropertyId
					int basePropertyIdDb = rs.getInt("BasePropertyId");
					int basePropertyIdResponse = jsonPath.getInt("listData["+i+"].BasePropertyId");
					if(basePropertyIdResponse == basePropertyIdDb) {
						// Reporter.log(", Correct value of BasePropertyId ["+basePropertyIdDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of BasePropertyId ["+basePropertyIdDb+"]", true);
						Assert.fail("InCorrect value of BasePropertyId ["+basePropertyIdDb+"]");
					}
					
					// check value of BasePropertyValue
					String basePropertyValueDb = rs.getString("BasePropertyValue");
					String basePropertyValueResponse = jsonPath.getString("listData["+i+"].BasePropertyValue");
					if(basePropertyValueResponse.equalsIgnoreCase(basePropertyValueDb)) {
						// Reporter.log(", Correct value of BasePropertyValue ["+basePropertyValueDb+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of BasePropertyValue ["+basePropertyValueDb+"]", true);
						Assert.fail("InCorrect value of BasePropertyValue ["+basePropertyValueDb+"]");
					}

					i++;
				}
			}else{
				// verify listData is blank
				String listDataResponse = jsonPath.getJsonObject("listData").toString();
				if(listDataResponse.equals("[]")) {
					// Reporter.log(", Correct listData []", true);
				}
				else{
					Reporter.log(", InCorrect listData ["+listDataResponse+"]", true);
					Assert.fail("InCorrect listData ["+listDataResponse+"]");
				}

				// verify message as null
				String messageResponse = jsonPath.getString("message");
				if(messageResponse == null) {
					// Reporter.log(", Correct message [null]", true);
				}
				else{
					Reporter.log(", InCorrect message ["+messageResponse+"]", true);
					Assert.fail("InCorrect message ["+messageResponse+"]");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_CollegeLinkSubsDetails(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}