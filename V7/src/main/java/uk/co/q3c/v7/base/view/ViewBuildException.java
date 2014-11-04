/*
 * Copyright (c) 2014 David Sowerby
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for
 * the specific language governing permissions and limitations under the License.
 */

package uk.co.q3c.v7.base.view;

/**
 * Created by david on 28/09/14.
 */
public class ViewBuildException extends RuntimeException {
    public ViewBuildException() {
    }

    public ViewBuildException(String message) {
        super(message);
    }

    public ViewBuildException(String message, Throwable cause) {
        super(message, cause);
    }

    public ViewBuildException(Throwable cause) {
        super(cause);
    }

    public ViewBuildException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}