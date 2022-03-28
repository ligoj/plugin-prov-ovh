# OVHCloud 
## Create token

- First, go to the OVHCloud website: https://www.ovhcloud.com/fr/. Then create an account.
- Then go to the website : https://eu.api.ovh.com/createToken/
- Once you have arrived on this site you should fill in the form below to create your tokens. 
![](https://i.imgur.com/BxE0IrE.png)  
enter your OVHCloud ID (warning the ID is not the email address but your ID ending with -ovh)
- Once your identifiers are filled in, you just have to give a name and a description to your script and then assign the rights and the duration of validity of this token.  
![](https://i.imgur.com/szdhG9m.png)  
- As soon as all filled click on create keys . 
- You will obtain your various token to preserve preciously. 
![](https://i.imgur.com/Qd06pxN.png)

## Postman 

- Fill your headers like this :![](https://i.imgur.com/u6HKlWp.png)
 In X-Ovh-Apllication put its Application Key generate .
- Then create your environment variables by clicking on the eye and then on add : ![](https://i.imgur.com/br0MdHK.png)
- One then created it is two variables : 

| variables | values |
| -----------------------------| --------------------- |
| lf_ovh_api_app_secret | Application Secret |
| lf_ovh_api_app_consumer_key | Consumer Key |

![](https://i.imgur.com/GrTaGvG.png)

- finally in pre-spirit added the code below: 
```
pm.sendRequest({
    url: "https://api.ovh.com/1.0/auth/time",
    method: "GET",
    headers: {
        'Content-Type': 'application/json; charset=utf-8'
    },
    body: {}
 },
 function (err, res) {

    pm.expect(err).to.not.be.ok;
    pm.expect(res).to.have.property('code', 200);
    pm.expect(res).to.have.property('status', 'OK');

    var serverTimestamp = res.text();
    postman.setGlobalVariable("lf_ovh_api_app_server_timestamp", serverTimestamp);

    var time_delta = serverTimestamp - Math.round(new Date().getTime()/1000);
    var now = Math.round(new Date().getTime()/1000) + time_delta;
    var body = '';

    postman.setGlobalVariable("lf_ovh_api_app_timestamp", now);

   var toSign = environment.lf_ovh_api_app_secret + '+' + environment.lf_ovh_api_consumer_key + '+' + pm.request.method + '+' + pm.request.url.toString() + '+' + body + '+' + now;

    var signature = '$1$' + CryptoJS.SHA1(toSign);

    postman.setGlobalVariable("lf_ovh_api_app_consumer_signature", signature);
 }
);
```