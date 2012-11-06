// Copyright (C) 2012 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.googleappsgroup;

import com.google.gdata.client.appsforyourdomain.AppsGroupsService;
import com.google.gdata.util.AuthenticationException;
import com.google.gerrit.common.data.GroupDescription;
import com.google.gerrit.common.data.GroupReference;
import com.google.gerrit.extensions.annotations.Listen;
import com.google.gerrit.reviewdb.client.AccountGroup;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.account.GroupBackend;
import com.google.gerrit.server.account.GroupMembership;
import com.google.gerrit.server.config.GerritServerConfig;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.jgit.lib.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * GroupBackend implementation for Google Apps domains.
 */
@Singleton
@Listen
public class GoogleAppsGroup implements GroupBackend {
  private static final Logger log = LoggerFactory
      .getLogger(GoogleAppsGroup.class);

  private static final String UUID_PREFIX = "google:";
  private static final String NAME_PREFIX = "google/";

  private final AppsGroupsService service;

  @Inject
  GoogleAppsGroup(@GerritServerConfig Config cfg) throws AuthenticationException {
    String user = cfg.getString("googleappsgroup", null, "user");
    String password = cfg.getString("googleappsgroup", null, "password");
    String domain = cfg.getString("googleappsgroup", null, "domain");
    service = new AppsGroupsService(user, password, domain,
        "gerrit-plugins-googleappsgroup");
  }

  @Override
  public boolean handles(AccountGroup.UUID uuid) {
    return uuid.get().startsWith(UUID_PREFIX);
  }

  @Override
  public GroupMembership membershipsOf(IdentifiedUser user) {
    return GroupMembership.EMPTY;
  }

  @Override
  public GroupDescription.Basic get(final AccountGroup.UUID uuid) {
    return null;
  }

  @Override
  public Collection<GroupReference> suggest(String name) {
    return Collections.emptyList();
  }
}
