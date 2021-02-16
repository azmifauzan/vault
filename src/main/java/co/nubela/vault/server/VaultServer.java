package co.nubela.vault.server;

import co.nubela.vault.json.ClientRequest;
import co.nubela.vault.json.ClientResponse;
import co.nubela.vault.json.InfuraRequest;
import co.nubela.vault.json.InfuraResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class VaultServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String gasPrice;

    public VaultServer(String gasPrice){
        this.gasPrice = gasPrice;
    }

    /*public void start(int port) {
        try {
            System.out.println("SERVER LISTENING ON PORT: "+port);
            serverSocket = new ServerSocket(port);
            clientSocket = serverSocket.accept();
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (".".equals(inputLine)) {
                    out.println("GOOD BYE");
                    break;
                }
                out.println(parse(inputLine));
            }
            stop();
        } catch (Exception e) {
            System.out.println("ERROR: "+e.getMessage());
        }
    }*/

    public void start(String path){
        File socketFile = new File(path);
        try (AFUNIXServerSocket server = AFUNIXServerSocket.newInstance()) {
            server.bind(new AFUNIXSocketAddress(socketFile));
            System.out.println("server: " + server);
            while (!Thread.interrupted()) {
                System.out.println("Waiting for connection...");
                try (Socket sock = server.accept()) {
                    System.out.println("Connected: " + sock);
                    /*try (InputStream is = sock.getInputStream(); //
                        OutputStream os = sock.getOutputStream()) {
                        byte[] buf = new byte[1024];
                        int read = is.read(buf);

                        String clientInput = new String(buf, 0, read, "UTF-8");
                        String response = getResponse(clientInput);

                        os.write(response.getBytes());
                        os.flush();
                    }*/
                    out = new PrintWriter(sock.getOutputStream(), true);
                    in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                    String inputLine  = in.readLine();
                    out.println(getResponse(inputLine));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getResponse(String clientInput) throws Exception {
        String result = "";

        try{
            //mapping request
            ObjectMapper mapper = new ObjectMapper();
            ClientRequest request = mapper.readValue(clientInput, ClientRequest.class);

            //get nonce from network
            InfuraRequest infuraRequest = new InfuraRequest();
            infuraRequest.setId(1);
            infuraRequest.setMethod("eth_getTransactionCount");
            List<String> params = new ArrayList<>();
            params.add(request.getTo_address());
            params.add("pending");
            infuraRequest.setParams(params);
            String json = mapper.writeValueAsString(infuraRequest);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"),json);
            Request requestClient = new Request.Builder()
                    .url("https://mainnet.infura.io/v3/56e09f7593134cc9bc4b3b099af69d3c")
                    .post(requestBody)
                    .build();
            OkHttpClient client = new OkHttpClient();
            Call call = client.newCall(requestClient);
            ResponseBody responseBody = call.execute().body();
            InfuraResponse infuraResponse = mapper.readValue(responseBody.string(), InfuraResponse.class);
            Long lnonce = Long.decode(infuraResponse.getResult());
            BigInteger nonce = BigInteger.valueOf(lnonce+1);

            //init trx
            BigDecimal amount = Convert.toWei(request.getAmount(), Convert.Unit.ETHER);
            BigInteger gaslimit = BigInteger.valueOf(1000);
            Long lGasPrice = Long.decode(gasPrice);
            BigDecimal gasprice = Convert.toWei(lGasPrice.toString(), Convert.Unit.WEI);
            MathContext mc = new MathContext(2);
            BigDecimal amountsent = amount.subtract(gasprice,mc);

            //create raw eth trx
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce,gasprice.toBigInteger(), gaslimit,request.getTo_address(), amountsent.toBigInteger());

            //get from_address private key
            String filePath = request.getFrom_address()+".key";
            String privateKey = new String(Files.readAllBytes(Paths.get(filePath)));
            Credentials credentials = Credentials.create(privateKey);

            //sign trx
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            //send response
            ClientResponse response = new ClientResponse();
            response.setId(request.getId());
            response.setTx(hexValue);
            result = mapper.writeValueAsString(response);
        } catch (Exception e) {
            throw e;
        }

        return result;
    }

    private String parse(String inputLine) {
        String result = "";
        try {
            //mapping request
            ObjectMapper mapper = new ObjectMapper();
            ClientRequest request = mapper.readValue(inputLine, ClientRequest.class);

            //init trx
            Long lAmount = Long.parseLong(request.getAmount());
            Long lGasPrice = Long.decode(gasPrice);
            BigInteger nonce = BigInteger.valueOf(0);
            BigInteger gaslimit = BigInteger.valueOf(1000);
            BigInteger gasprice = BigInteger.valueOf(lGasPrice);
            BigInteger amountsent = BigInteger.valueOf(lAmount - lGasPrice);

            //create raw eth trx
            RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce,gasprice, gaslimit,request.getTo_address(), amountsent);

            //get from_address private key
            String filePath = request.getFrom_address()+".key";
            String privateKey = new String(Files.readAllBytes(Paths.get(filePath)));
            Credentials credentials = Credentials.create(privateKey);

            //sign trx
            byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
            String hexValue = Numeric.toHexString(signedMessage);

            //send response
            ClientResponse response = new ClientResponse();
            response.setId(request.getId());
            response.setTx(hexValue);
            result = mapper.writeValueAsString(response);
        }
        catch (Exception ex){
            result = "ERROR: "+ex.getMessage();
        }

        return result;
    }

    public void stop() {
        try {
            in.close();
            out.close();
            clientSocket.close();
            serverSocket.close();
        } catch (IOException e) {
            System.out.println("ERROR: "+e.getMessage());
        }
    }
}
