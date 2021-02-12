package co.nubela.vault.server;

import co.nubela.vault.json.ClientRequest;
import co.nubela.vault.json.ClientResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.utils.Numeric;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;

public class VaultServer {
    private ServerSocket serverSocket;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private String gasPrice;

    public VaultServer(String gasPrice){
        this.gasPrice = gasPrice;
    }

    public void start(int port) {
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
