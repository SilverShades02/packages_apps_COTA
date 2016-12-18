/*
 * Copyright 2014 ParanoidAndroid Project
 *
 * This file is part of CypherOS OTA.
 *
 * CypherOS OTA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CypherOS OTA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CypherOS OTA.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.cypher.cota.updater;

import com.cypher.cota.updater.Updater.PackageInfo;
import com.cypher.cota.utils.Version;

import org.json.JSONObject;

import java.util.List;

public interface Server {

    String getUrl(String device, Version version);

    List<PackageInfo> createPackageInfoList(JSONObject response) throws Exception;

    String getError();
}
