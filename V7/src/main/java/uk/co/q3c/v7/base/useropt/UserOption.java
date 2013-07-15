/*
 * Copyright (C) 2013 David Sowerby
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.co.q3c.v7.base.useropt;

import org.joda.time.DateTime;

public interface UserOption {

	public void setOption(String optionGroup, String option, int value);

	public void setOption(String optionGroup, String option, String value);

	public void setOption(String optionGroup, String option, DateTime value);

	public void setOption(String optionGroup, String option, double value);

	public int getOptionAsInt(String optionGroup, String option, int defaultValue);

	public String getOptionAsString(String optionGroup, String option, String defaultValue);

	public DateTime getOptionAsDateTime(String optionGroup, String option, DateTime defaultValue);

	public double getOptionAsDouble(String optionGroup, String option, double defaultValue);

}
