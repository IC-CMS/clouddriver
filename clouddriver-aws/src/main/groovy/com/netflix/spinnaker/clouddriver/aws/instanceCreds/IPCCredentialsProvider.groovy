/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.clouddriver.aws.instanceCreds

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.BasicSessionCredentials
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

import java.text.SimpleDateFormat

@Slf4j
class IPCCredentialsProvider implements AWSCredentialsProvider {

  private final String address
  private final String iamRole

  private Date expiration
  private AWSCredentials credentials

  private static final String DEFAULT_ADDRESS = "http://169.254.169.254/latest/meta-data/iam/security-credentials"

  IPCCredentialsProvider(String address, String iamRole){
    this.iamRole = iamRole

    if (StringUtils.isBlank(address)){
      this.address = DEFAULT_ADDRESS
    }
    else{
      this.address = address
    }
  }

  @Override
  AWSCredentials getCredentials() {
    if (!expiration || expiration.before(new Date())) {
      this.credentials = getRemoteCredentials()
    }
    return this.credentials
  }

  @Override
  void refresh() {
    this.credentials = getRemoteCredentials()
  }

  private getRemoteCredentials(){
    UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(this.address + "/" + iamRole)
    RestTemplate rt = new RestTemplate()
    IPCCredentials credentials = rt.getForObject(builder.toUriString(), IPCCredentials.class)
    log.debug("Received the following instance profile credentials (token removed): " + credentials.toString())

    expiration = credentials.getExpiration()
    return new BasicSessionCredentials(credentials.accessKeyId, credentials.getSecretAccessKey(), credentials.getToken())
  }

}
