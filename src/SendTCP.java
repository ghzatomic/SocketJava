import java.net.InetAddress;  
  
import jpcap.*;  
import jpcap.packet.EthernetPacket;  
import jpcap.packet.IPPacket;  
import jpcap.packet.TCPPacket;  
  
class SendTCP  
{  
    public static void main(String[] args) throws java.io.IOException{  
        NetworkInterface[] devices = JpcapCaptor.getDeviceList();  
        if(args.length<1){  
            System.out.println("Usage: java SentTCP <device index (e.g., 0, 1..)>");  
            for(int i=0;i<devices.length;i++)  
                System.out.println(i+":"+devices[i].name+"("+devices[i].description+")");  
            //System.exit(0);  
        }  
        int index=Integer.parseInt("0");  
        JpcapSender sender=JpcapSender.openDevice(devices[index]);  
  
        TCPPacket p=new TCPPacket(57235,1057,5000,78,false,false,false,false,false,false,false,false,64400,10);  
    p.setIPv4Parameter(0,false,false,false,0,false,true,false,0,24300,118,IPPacket.IPPROTO_TCP,  
            InetAddress.getByName("192.168.1.118"),InetAddress.getByName("192.168.1.118"));  
    p.data=("<Gabriel.> oi.").getBytes();  
          
        EthernetPacket ether=new EthernetPacket();  
        ether.frametype=EthernetPacket.ETHERTYPE_IP;  
        ether.src_mac=new byte[]{(byte)0,(byte)36,(byte)140,(byte)137,(byte)152,(byte)24};  
        ether.dst_mac=new byte[]{(byte)116,(byte)234,(byte)58,(byte)233,(byte)236,(byte)227};  
        p.datalink=ether;  
  
        for(int i=0;i<10;i++)  
            sender.sendPacket(p);  
    }  
} 