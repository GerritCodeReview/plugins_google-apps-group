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
import com.google.gdata.data.appsforyourdomain.generic.GenericEntry;
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
import java.util.Set;

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
    final Set<String> emails = user.getEmailAddresses();
    if (emails.isEmpty()) {
      return GroupMembership.EMPTY;
    }

    return new GroupMembership() {
      @Override
      public boolean contains(AccountGroup.UUID uuid) {
        for (String email : emails) {
          try {
            if (service.isMember(idOf(uuid), email)) {
              return true;
            }
          } catch (Exception e) {
            log.warn(String.format("isMember(%s, %s)", uuid, email), e);
          }
        }
        return false;
      }

      @Override
      public boolean containsAnyOf(Iterable<AccountGroup.UUID> uuids) {
        for (AccountGroup.UUID uuid : uuids) {
          if (contains(uuid)) {
            return true;
          }
        }
        return false;
      }

      @Override
      public Set<AccountGroup.UUID> getKnownGroups() {
        return Collections.emptySet();
      }
    };
  }

  @Override
  public GroupDescription.Basic get(final AccountGroup.UUID uuid) {
    if (!uuid.get().startsWith(UUID_PREFIX)) {
      return null;
    }
    return retrieveGroup(idOf(uuid));
  }

  private GroupDescription.Basic retrieveGroup(String query) {
    try {
      GenericEntry entry = service.retrieveGroup(query);
      return groupFor(entry);
    } catch (Exception e) {
      log.warn(String.format("retrieveGroup(%s)", query), e);
      return null;
    }
  }

  @Override
  public Collection<GroupReference> suggest(String name) {
    GroupDescription.Basic group = null;
    if (name.startsWith(UUID_PREFIX)) {
      group = retrieveGroup(name.substring(UUID_PREFIX.length()));
    } else if (name.startsWith(NAME_PREFIX)) {
      group = retrieveGroup(name.substring(NAME_PREFIX.length()));
    }

    if (group != null) {
      return Collections.singleton(GroupReference.forGroup(group));
    }
    return Collections.emptyList();
  }

  private static String idOf(AccountGroup.UUID uuid) {
    return uuid.get().substring(UUID_PREFIX.length());
  }

  private static GroupDescription.Basic groupFor(GenericEntry entry) {
    final String groupId =
        entry.getProperty(AppsGroupsService.APPS_PROP_GROUP_ID);
    if (groupId == null) {
      return null;
    }

    return new GroupDescription.Basic() {
      @Override
      public AccountGroup.UUID getGroupUUID() {
        return new AccountGroup.UUID(UUID_PREFIX + groupId);
      }

      @Override
      public String getName() {
        return NAME_PREFIX + groupId;
      }

      @Override
      public boolean isVisibleToAll() {
        return true;
      }

      @Override
      public String toString() {
        return String.format("GoogleAppsGroup[uuid=%s name=%s]", getGroupUUID(), getName());
      }
    };
  }
}
