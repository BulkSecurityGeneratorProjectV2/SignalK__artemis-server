/**
 * Copyright 2016 Signal K <info@signalk.org> and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

var utils = require('@signalk/nmea0183-utilities');

/*
22  02  XX  XX  00  Total Mileage: XXXX/10 nautical miles
*/

module.exports = function (input) {
  var id = input.id,
      sentence = input.sentence,
      parts = input.parts,
      tags = input.tags;

  var nauticalMilesToMeters = 1852;
  var totalMileage = (parseInt(parts[2], 16) + 256 * parseInt(parts[3], 16)) * nauticalMilesToMeters / 10.0;
  var pathValues = [];

  pathValues.push({
    path: 'navigation.log',
    value: utils.float(totalMileage)
  });

  return {
    updates: [{
      source: tags.source,
      timestamp: tags.timestamp,
      values: pathValues
    }]
  };
};