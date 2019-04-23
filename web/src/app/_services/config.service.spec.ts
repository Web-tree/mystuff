import {TestBed} from "@angular/core/testing";

import {ConfigService} from "./config.service";
import {environment} from "../../environments/environment";
import {HttpClientTestingModule, HttpTestingController} from "@angular/common/http/testing";

describe('ConfigService', () => {

  let configService: ConfigService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConfigService],
      imports: [HttpClientTestingModule]
    });
    configService = TestBed.get(ConfigService);
    httpMock = TestBed.get(HttpTestingController);
  });

  it('#getBackUrl should return expected value', () => {
    const url = "http://localhost:9000/";
    expect(configService.getBackUrl()).toEqual(url);
  });

  it('#getBackUrl should return value config.json if reset', () => {
    let urlFromConfig = "someUrlFromConfig";

    configService.init().then(() => {
      expect(environment.backUrl).toEqual(urlFromConfig)
    });
    httpMock.expectOne("/config.json").flush({"backUrl": urlFromConfig});
  });
});
