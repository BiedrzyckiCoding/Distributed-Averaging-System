import java.net.*;
import java.util.*;

public class DAS {
    public static void main(String[] args) {
        //validate the number of arguments
        if (args.length != 2) {
            System.err.println("Usage: java DAS <port> <number>");
            System.exit(1);
        }

        int port = 0;
        double number = 0;

        //parse the arguments
        try {
            port = Integer.parseInt(args[0]);
            number = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("<port> musy be an integer and <number> must be a double.");
            System.exit(1);
        }

        //proceed to determine mode of operation
        try {
            DatagramSocket socket = new DatagramSocket(port);
            //master mode
            runAsMaster(port, number, socket);
        } catch (SocketException e) {
            //slave mode
            runAsSlave(port, number);
        }
    }

    public static void runAsMaster(int port, double number, DatagramSocket socket) {
        System.out.println("application starting in Master Mode");
        List<Double> numbers = new ArrayList<>();
        final double EPSILON = 1e-9; //im using this because I would like to avoid any issues while comparing double values
        //since the values are passed as dobles.

        if (Math.abs(number) > EPSILON && Math.abs(number + 1) > EPSILON) {
            numbers.add(number);
        }

        while (true) {
            try {
                byte[] buffer = new byte[1024];//new buffer made every time the master recieves a message to acoid data corruption
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                //we need to ignore packets sent from our own port to avoid processing our own broadcasts
                if (packet.getPort() == socket.getLocalPort()) {
                    System.out.println("ignored packet from own port.");
                    continue;
                }


                //process the received packet
                String received = new String(packet.getData(), 0, packet.getLength()).trim();
                double receivedNumber;
                try {
                    receivedNumber = Double.parseDouble(received);
                } catch (NumberFormatException e) {
                    System.err.println("Received invalid number: " + received);
                    continue;
                }

                //wanted to make it correct in every scenario, that's why I use this epsilon value
                if (Math.abs(receivedNumber) > EPSILON && Math.abs(receivedNumber + 1) > EPSILON) {
                    //the number is neither 0 nor -1
                    System.out.println("Received number: " + receivedNumber);
                    numbers.add(receivedNumber);
                    //print the entire numbers list
                    System.out.println("Current list of numbers: " + numbers);
                } else if (Math.abs(receivedNumber) < EPSILON) {
                    //the number is 0
                    calculateAndBroadcastAverage(port, socket, numbers);
                } else if (Math.abs(receivedNumber + 1) < EPSILON) {
                    //the number is -1
                    System.out.println("Received -1. Terminating.");
                    broadcastMessage(port, socket, "-1");
                    socket.close();
                    break;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static void runAsSlave(int port, double number) {
        System.out.println("application running in Slave Mode");
        try {
            DatagramSocket socket = new DatagramSocket();//OS assigns a random port, read that on docks
            byte[] buffer = String.valueOf(number).getBytes();
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length,
                    InetAddress.getByName("localhost"), port
            );
            System.out.println("sent through port: " + socket.getLocalPort());
            socket.send(packet);//sending message
            socket.close();//closing and termination of the sockets
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void calculateAndBroadcastAverage(int port, DatagramSocket socket, List<Double> numbers) {
        if (!numbers.isEmpty()) {
            double sum = 0;
            for (double num : numbers) {
                sum += num;
            }
            double average = sum / numbers.size();
            int roundedAverage = (int) Math.floor(average);
            System.out.println("Average: " + roundedAverage);

            broadcastMessage(port, socket, String.valueOf(roundedAverage));
        } else {
            System.out.println("No numbers to average.");
        }
    }

    public static void broadcastMessage(int port, DatagramSocket socket, String message) {
        try {
            socket.setBroadcast(true);
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    buffer, buffer.length,
                    InetAddress.getByName("255.255.255.255"), port
            );
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
