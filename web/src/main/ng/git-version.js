const { gitDescribeSync } = require('git-describe');
const { writeFileSync } = require('fs');

const gitInfo = gitDescribeSync();
gitInfo.date = new Date();
const versionInfoJson = JSON.stringify(gitInfo, null, 2);

writeFileSync('git-version.json', versionInfoJson);