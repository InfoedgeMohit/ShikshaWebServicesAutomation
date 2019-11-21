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

public class GetProductFeatures extends Common {

	static String apiPath;
	String api;
	static String[] params;
	static String[] paramType;
	Response apiResponse = null;
	String apiResponseMsgExpected;
	static String testDataFilePath;
	String prodId;

	static ArrayList<Integer> responseTimes = new ArrayList<Integer>();

	@BeforeClass
	public void doItBeforeTest() {
		apiResponseExpectedTime = 1000; // in MS
		// api path
		apiPath = "sums/v1/info/getProductFeatures";
		api = serverPath + apiPath;
		// API params in sequence followed by apiResponseMsgExpected
		params = new String[]{"prodId", "apiResponseMsgExpected"};
		// type of API params in sequence
		paramType = new String[]{"Integer"};
		// test data file
		testDataFilePath = "//src//test/resources//sums//info//GetProductFeatures.xlsx";
	}

	@DataProvider
	public static String[][] testData() throws Exception {
		// The number of times data is repeated, test will be executed the same no. of times
		String[][] testData = prepareTestData(params, testDataFilePath);
		return testData;
	}

	@Test(dataProvider = "testData", priority = 1)
	public void verifyProductFeaturesApi(String prodId, String apiResponseMsgExpected) {
		this.apiResponseMsgExpected = apiResponseMsgExpected;
		this.prodId = prodId;

		// pass api params and hit api
		if(!prodId.equals("ignoreInput"))
			apiResponse = RestAssured.given().param("prodId",prodId).when().post(api).then().extract().response();
		else if (prodId.equals("ignoreInput"))
			apiResponse = RestAssured.given().when().post(api).then().extract().response();

		// fetch status code from response
		int statusCode = apiResponse.getStatusCode();
		JsonPath jsonPath = apiResponse.jsonPath();

		if(statusCode == 200){
			// In Positive cases, verify values of status, message and listData[from Db]
			// Reporter.log("Positive case", true);
			verifyPostiveCases(prodId, jsonPath);
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

	public void verifyPostiveCases(String prodId, JsonPath jsonPath){
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

			// if prodId is blank or prodId not provided, then details of all products need to be fetched
			String subQuery = "";
			if(!prodId.equals("") && !prodId.equalsIgnoreCase("ignoreInput"))
				subQuery = "and bp.BaseProductId = "+prodId+"";

			// fetch details from Base_Products, Base_Prod_Property_Mapping and Base_Prod_Properties
			ResultSet rs = exceuteDbQuery("select bp.BaseProductId, bp.BaseProdCategory, bp.baseProdSubCategory, bpp.BasePropertyName, bppm.BasePropertyValue from "
					+ "Base_Products bp, Base_Prod_Property_Mapping bppm, Base_Prod_Properties bpp where bp.BaseProductId = bppm.BaseProductId and "
					+ "bppm.BasePropertyId = bpp.BasePropertyId "+subQuery+" order by bp.BaseProductId;","SUMS");

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

				// Total count of base products
				int baseProdCount = 680;
				if(!prodId.equals("") && !prodId.equalsIgnoreCase("ignoreInput"))
					baseProdCount = 1;

				// verify details in listData
				int i=0;
				rs.next();
				while(i < baseProdCount){
					// Reporter.log("<br> Veriify data of listData["+i+"] >>", true);
					// check value of baseProductId
					int baseProductIdDb = rs.getInt("BaseProductId");
					int baseProductIdResponse = jsonPath.getInt("listData["+i+"].baseProductId");
					if(baseProductIdResponse == baseProductIdDb) {
						// Reporter.log(", Correct value of baseProductId ["+baseProductIdResponse+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of baseProductId ["+baseProductIdResponse+"]", true);
						Assert.fail("InCorrect value of baseProductId ["+baseProductIdResponse+"]");
					}

					// check value of baseProdCategory
					String baseProdCategoryDb = rs.getString("BaseProdCategory");
					String baseProdCategoryResponse = jsonPath.getString("listData["+i+"].BaseProdCategory");
					if(baseProdCategoryResponse.equalsIgnoreCase(baseProdCategoryDb)) {
						// Reporter.log(", Correct value of baseProdCategory ["+baseProdCategoryResponse+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of baseProdCategory ["+baseProdCategoryResponse+"]", true);
						Assert.fail("InCorrect value of baseProdCategory ["+baseProdCategoryResponse+"]");
					}

					// check value of baseProdSubCategory
					String baseProdSubCategoryDb = rs.getString("baseProdSubCategory");
					String baseProdSubCategoryResponse = jsonPath.getString("listData["+i+"].BaseProdSubCategory");
					if(baseProdSubCategoryResponse.equalsIgnoreCase(baseProdSubCategoryDb)) {
						// Reporter.log(", Correct value of baseProdSubCategory ["+baseProdSubCategoryResponse+"]", true);
					}
					else{
						Reporter.log(", InCorrect value of baseProdSubCategory ["+baseProdSubCategoryResponse+"]", true);
						Assert.fail("InCorrect value of baseProdSubCategory ["+baseProdSubCategoryResponse+"]");
					}

					// get count of baseProperties
					ResultSet rs1 = exceuteDbQuery("select count(*) count from Base_Products bp, Base_Prod_Property_Mapping bppm, Base_Prod_Properties bpp where bp.BaseProductId = bppm.BaseProductId and bppm.BasePropertyId = bpp.BasePropertyId and bp.BaseProductId = "+baseProductIdDb+";", "SUMS");
					rs1.next();
					int propertyCount = rs1.getInt("count");

					// verify values of baseProperties
					int j=0;
					rs.previous();
					while(rs.next() && j < propertyCount){
						String basePropertyNameDb = rs.getString("BasePropertyName");
						String basePropertyValueDb = rs.getString("BasePropertyValue");
						System.out.println(jsonPath.getString("listData["+i+"].property"));
						String basePropertyValueResponse = jsonPath.getString("listData["+i+"].property."+basePropertyNameDb+"");
						if(basePropertyValueResponse.equalsIgnoreCase(basePropertyValueDb)) {
							// Reporter.log(", Correct value of "+basePropertyNameDb+" ["+basePropertyValueResponse+"]", true);
						}
						else{
							Reporter.log(", InCorrect value of "+basePropertyNameDb+" ["+basePropertyValueResponse+"]", true);
							Assert.fail("InCorrect value of "+basePropertyNameDb+" ["+basePropertyValueResponse+"]");
						}
						j++;
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
	public static void respnseTimeStats_GetProductFeatures(){
		respnseTimeStatsCommon(responseTimes, apiPath.substring(apiPath.lastIndexOf("/")+1));
	}
}