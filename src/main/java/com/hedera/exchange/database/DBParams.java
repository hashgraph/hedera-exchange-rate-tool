package com.hedera.exchange.database;

/*-
 * ‌
 * Hedera Exchange Rate Tool
 * ​
 * Copyright (C) 2019 - 2020 Hedera Hashgraph, LLC
 * ​
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ‍
 *
 * Copyright (c) 2007-present, Stephen Colebourne & Michael Nascimento Santos.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of JSR-310 nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.hedera.exchange.ERTUtils;
import com.hedera.exchange.Environment;
import com.hedera.exchange.exchanges.Exchange;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static com.hedera.exchange.ExchangeRateTool.env;

public final class DBParams {
	private static final Logger LOGGER = LogManager.getLogger(DBParams.class);

	public DBParams() {
		throw new UnsupportedOperationException("Utility Class");
	}

	public static String getEndpoint() {
		if (env == Environment.AWS) {
			return ERTUtils.getDecryptedEnvironmentVariableFromAWS("ENDPOINT") + getDatabaseName();
		}
		else {
			LOGGER.error(Exchange.EXCHANGE_FILTER, "should not be possible");
			return System.getenv("ENDPOINT") + getDatabaseName();
		}
	}

	public static String getUsername() {
		if (env == Environment.AWS) {
			return ERTUtils.getDecryptedEnvironmentVariableFromAWS("USERNAME");
		}
		else {
			return System.getenv("USERNAME");
		}
	}

	public static String getPassword() {
		if (env == Environment.AWS) {
			return ERTUtils.getDecryptedEnvironmentVariableFromAWS("PASSWORD");
		}
		else {
			return System.getenv("PASSWORD");
		}
	}

	public static String getDatabaseName() {
		if (env == Environment.AWS) {
			return ERTUtils.getDecryptedEnvironmentVariableFromAWS("DATABASE");
		}
		else {
			return System.getenv("DATABASE");
		}
	}

	public static Connection getConnection() throws SQLException {
		if (env == Environment.AWS) {
			return DriverManager.getConnection(getEndpoint(), getUsername(), getPassword());
		} else {
			DataSource pool = getGcpDataSource();
			return pool.getConnection();
		}
	}

	public static DataSource getGcpDataSource() {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(String.format("jdbc:postgresql:///%s", getDatabaseName()));
		config.setUsername(getUsername());
		config.setPassword(getPassword());
		config.addDataSourceProperty("socketFactory", "com.google.cloud.sql.postgres.SocketFactory");
		config.addDataSourceProperty("cloudSqlInstance", System.getenv("INSTANCE_CONNECTION_NAME"));
		config.addDataSourceProperty("ipTypes", "PUBLIC,PRIVATE");
		return new HikariDataSource(config);
	}
}
