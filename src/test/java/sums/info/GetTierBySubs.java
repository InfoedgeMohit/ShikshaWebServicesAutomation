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

public class GetTierBySubs extends Common {

	static String apiPath = "sums/v1/info/getTierBySubs";
	String api = serverPath + apiPath;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	String subsId;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"subsId", "apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"int"};
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// test data file
		String testDataFilePath = "//src//test/resources//sums//info//GetTierBySubs.xlsx";
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyGetTierBySubsApi(String subsId, String apiResponseMsgExpected) {
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
			} else {
				Reporter.log(", InCorrect status ["+statusResponse+"]", true);
				Assert.fail("InCorrect status ["+statusResponse+"]");
			}
			
			// fetch BasePropertyNames of subsId from db [groupBy is used because if same type of tier, then 1st value will be returned only]
			ResultSet rs = exceuteDbQuery("select bpp.BasePropertyName, substring(bpp.BasePropertyName, 8,12) a from Subscription s, "
					+ "Base_Prod_Properties bpp where bpp.BasePropertyId in (select BasePropertyId from Base_Prod_Property_Mapping where "
					+ "BaseProductId  = s.BaseProductId and BasePropertyId in (11, 12, 13, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36)) and s.SubscriptionId = "+subsId+" group by a;","SUMS");
			
			// get row count of resultSet of sql query
			int rowcount = getCountOfResultSet(rs);

			if(rowcount > 0){
				// verify message is null
				String messageResponse = jsonPath.getString("message");
				if(messageResponse == null) {
					// Reporter.log(", Correct message [null]", true);
				} else{
					Reporter.log(", InCorrect message ["+messageResponse+"]", true);
					Assert.fail("InCorrect message ["+messageResponse+"]");
				}

				// verify values of city_tier/subcat_tier/state_tier/country_tier in mapData
				while(rs.next()){
					String str = rs.getString("BasePropertyName");
					
					if(str.contains("City")){
						// check value of city_tier
						int cityTierDb = Integer.parseInt(str.substring(5, 6));
						int cityTierResponse = jsonPath.getInt("mapData.city_tier");
						if(cityTierResponse == cityTierDb) {
							// Reporter.log(", Correct value of city_tier ["+cityTierDb+"]", true);
						} else{
							Reporter.log(", InCorrect value of city_tier ["+cityTierDb+"]", true);
							Assert.fail("InCorrect value of city_tier ["+cityTierDb+"]");
						}
					}else if(str.contains("Sub_Category")){
						// check value of subcat_tier
						int subCatTierDb = Integer.parseInt(str.substring(5, 6));
						int subCatTierResponse = jsonPath.getInt("mapData.subcat_tier");
						if(subCatTierResponse == subCatTierDb) {
							// Reporter.log(", Correct value of subcat_tier ["+subCatTierDb+"]", true);
						} else{
							Reporter.log(", InCorrect value of subcat_tier ["+subCatTierDb+"]", true);
							Assert.fail("InCorrect value of subcat_tier ["+subCatTierDb+"]");
						}
					}else if(str.contains("State")){
						// check value of state_tier
						int stateDb = Integer.parseInt(str.substring(5, 6));
						int stateResponse = jsonPath.getInt("mapData.state_tier");
						if(stateResponse == stateDb) {
							// Reporter.log(", Correct value of state_tier ["+stateDb+"]", true);
						} else{
							Reporter.log(", InCorrect value of state_tier ["+stateDb+"]", true);
							Assert.fail("InCorrect value of state_tier ["+stateDb+"]");
						}
					}else if(str.contains("Country")){
						// check value of country_tier
						int countryDb = Integer.parseInt(str.substring(5, 6));
						int countryResponse = jsonPath.getInt("mapData.country_tier");
						if(countryResponse == countryDb) {
							// Reporter.log(", Correct value of country_tier ["+countryDb+"]", true);
						} else{
							Reporter.log(", InCorrect value of country_tier ["+countryDb+"]", true);
							Assert.fail("InCorrect value of country_tier ["+countryDb+"]");
						}
					}
				}
			}else{
				// verify mapData is blank
				String mapDataResponse = jsonPath.getJsonObject("mapData").toString();
				if(mapDataResponse.equals("{}")) {
					// Reporter.log(", Correct mapData [{}]", true);
				} else{
					Reporter.log(", InCorrect mapData ["+mapDataResponse+"]", true);
					Assert.fail("InCorrect mapData ["+mapDataResponse+"]");
				}

				// verify message
				String messageResponse = jsonPath.getString("message");
				if(messageResponse.contentEquals("This subscription id does not belongs to any tier.")) {
					// Reporter.log(", Correct message [This subscription id does not belongs to any tier.]", true);
				} else{
					Reporter.log(", InCorrect message ["+messageResponse+"]", true);
					Assert.fail("InCorrect message ["+messageResponse+"]");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Test (priority = 2)
	public static void respnseTimeStats_GetTierBySubs(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}