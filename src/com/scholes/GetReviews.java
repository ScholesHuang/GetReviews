package com.scholes;

import java.io.FileNotFoundException;

public interface GetReviews {
	void exportAllData() throws Exception;
	String rawReviewsData(int pageNum);
}
