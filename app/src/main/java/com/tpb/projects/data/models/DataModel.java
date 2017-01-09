/*
 * Copyright  2016 Theo Pearson-Bray
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.tpb.projects.data.models;

/**
 * Created by theo on 15/12/16.
 */

public abstract class DataModel {

    static final String ID = "id";
    static final String NAME = "name";
    static final String CREATED_AT = "created_at";
    static final String UPDATED_AT = "updated_at";
    static final String URL = "url";

    long createdAt;

    public abstract long getCreatedAt();

}
