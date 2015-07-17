package io.finch.petstore

import io.finch.test.ServiceIntegrationSuite
import org.scalatest.Matchers
import org.scalatest.fixture.FlatSpec

class PetstorePetServiceIntegrationTest extends FlatSpec with Matchers
 with ServiceIntegrationSuite with PetstorePetServiceSuite {
  override val port: Int = 8123
}

class PetstoreStoreServiceIntegrationTest extends FlatSpec with Matchers
 with ServiceIntegrationSuite with PetstoreStoreServiceSuite {
  override val port: Int = 8124
}

class PetstoreUserServiceIntegrationTest extends FlatSpec with Matchers
 with ServiceIntegrationSuite with PetstoreUserServiceSuite {
  override val port: Int = 8125
}