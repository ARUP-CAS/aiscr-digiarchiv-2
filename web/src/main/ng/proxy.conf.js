const PROXY_CONFIG = {
  "/api/**": {
    "target": "http://localhost:8983/solr/dokument/select",
    "logLevel": "debug",
    "router": function (req) {
      req.headers["X-Custom-Header"] = "yes";
      if (req.path.indexOf('/search/akce') > -1) {
        return 'http://localhost:8983/solr/export/select?fq=doctype:akce&q=ident_cely:"' + req.query.id + '"&rows=100';
      } else if (req.path.indexOf('/search/lokalita') > -1) {
        return 'http://localhost:8983/solr/export/select?fq=doctype:lokalita&q=ident_cely:"' + req.query.id + '"&rows=100';

      } else if (req.path.indexOf('/search/dokjednotky') > -1) {
        return 'http://localhost:8983/solr/export/select?fq=doctype:dokumentacni_jednotka&q=parent:"' + req.query.id + '"&rows=100';

      } else if (req.path.indexOf('/search/externiodkaz') > -1) {
        return 'http://localhost:8983/solr/relations/select?fq=doctype:externi_odkaz&q=vazba:"' + req.query.id + '"&rows=100';
        
      } else if (req.path.indexOf('/search/externizdroj') > -1) {
        return 'http://localhost:8983/solr/export/select?fq=doctype:externi_zdroj&q=ident_cely:"' + req.query.id + '"&rows=100';
        
      } else if (req.path.indexOf('/search/nalezkomponentadok') > -1) {
        return 'http://localhost:8983/solr/relations/select?q=komponenta_dokument:"' + req.query.id + '"&rows=100';
        
      } else if (req.path.indexOf('/search/getkomponenty') > -1) {
        return 'http://localhost:8983/solr/export/select?fq=doctype:komponenta&q=parent:"' + req.query.id + '"&rows=100';

      } else if (req.path.indexOf('/search/getheslar') > -1) {
        return 'http://localhost:8983/solr/translations/select?q=heslar:"' + req.query.id + '"&rows=1000';

      } else if (req.path.indexOf('/search/query') > -1) {
        // let url = 'http://localhost:8983/solr/dokument/full?q=' + req.query.q;
        // if (req.query.page) {
        //   url += '&start=' + (req.query.page * 20);
        // }
        // return url; 
        let url = 'http://localhost:8082/amcr/api/search/query';
        if(Object.keys(req.query).length > 0) {
          url += '?' + req.originalUrl.split("?")[1];
        }
        return url; 

      } else if (req.path.indexOf('/search/mapa') > -1) {
        let url = 'http://localhost:8082/amcr/api/search/mapa';
        if(Object.keys(req.query).length > 0) {
          url += '?' + req.originalUrl.split("?")[1];
        }
        return url; 
      } else if (req.path.indexOf('/search/obdobi') > -1) {
        return 'http://localhost:8983/solr/heslar/select?fq=heslar_name:obdobi_prvni&q=*:*&rows=1000&sort=poradi%20asc&stats.field=poradi&stats=true&';
      } 

      
    },
    "bypass": function (req) {
      req.headers["X-Custom-Header"] = "yes";
      if (req.path.indexOf('/assets') > -1) {
        return req.url;
      }
      if (req.path.indexOf('/search/obdobi') > -1) {
         return "/mock/obdobi.json";
      } else if (req.path.indexOf('/search/query') > -1) {
        if (!req.query.q) {
          return "/mock/results.json";
        }
        
      } else if (req.path.indexOf('/search/mapa') > -1) {
        return "/mock/mapa.json";
      } else if (req.path.indexOf('/search/home') > -1) {
        return "/mock/home.json";
      } else if (req.path.indexOf('/search/akce') > -1) {
        console.log(req.url);
      } else if (req.path.indexOf('/search') > -1) {
        return "/mock/empty.json";
      } else if (req.path.indexOf('/users/login') > -1) {
        if (req.method === 'POST') {
          req.method = 'GET';
        }
        return "/mock/user.json";
      } else if (req.path.indexOf('/users/views') > -1) {
        return "/mock/views.json";
      } else if (req.path.indexOf('/pdf') > -1) {
        return '/mock/pdf/' + req.query.page + '.jpg';
      } else if (req.path.indexOf('/img') > -1) {
        return "/mock/empty.gif";
      }
    },
    "pathRewrite": {
      //"^/api":"",
      "^(.*)":""
    },
    "changeOrigin": false,
    "secure": false
  }
};
module.exports = PROXY_CONFIG;
