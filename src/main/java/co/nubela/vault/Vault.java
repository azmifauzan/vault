package co.nubela.vault;

import co.nubela.vault.json.GasPriceResponse;
import co.nubela.vault.json.InfuraRequest;
import co.nubela.vault.server.VaultServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.util.ArrayList;

public class Vault {
    public static void main(String[] args) {
        //get the gas price
        GasPriceResponse gas = null;
        try {
            InfuraRequest req = new InfuraRequest();
            req.setId(1);
            req.setMethod("eth_gasPrice");
            req.setParams(new ArrayList<>());
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(req);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),json);
            Request request = new Request.Builder()
                    .url("https://mainnet.infura.io/v3/56e09f7593134cc9bc4b3b099af69d3c")
                    .post(requestBody)
                    .build();
            OkHttpClient client = new OkHttpClient();
            Call call = client.newCall(request);
            ResponseBody response = call.execute().body();

            gas = mapper.readValue(response.string(), GasPriceResponse.class);
        }
        catch (Exception ex){
            System.out.println("ERROR: "+ex.getMessage());
        }

        //start the server
        VaultServer server = new VaultServer(gas.getResult());
        server.start(Integer.valueOf(args[0]));
    }
}
