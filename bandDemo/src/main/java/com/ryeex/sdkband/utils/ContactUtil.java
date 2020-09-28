package com.ryeex.sdkband.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;

import androidx.core.app.ActivityCompat;


import java.util.regex.Pattern;

import static android.Manifest.permission.READ_CONTACTS;

public class ContactUtil {

    private static final String TAG = "groot-contact";

    /**
     * 匹配格式: +xxx-xxxx-xxxx 或纯数字
     */
    private static final String REGEX_NUMBER = "^\\+?\\d+[-|\\d]+$";


    /**
     * 国家区号
     */
    private static int[] countryCodes = new int[]{
            86,//中国
            852,//香港
            853,//澳门
            886,//中国台湾
            1340,//美属维尔京群岛
            1670,//北马里亚纳群岛
            1671,//关岛
            1684,//美属萨摩亚
            1787, 939,//波多黎各

            1242,//巴哈马
            1246,//巴巴多斯
            1264,//安圭拉
            1268,//安提瓜和巴布达
            1284,//英属维尔京群岛
            1345,//开曼群岛
            1441,//百慕大
            1473,//格林纳达
            1649,//特克斯和凯科斯群岛
            1664,//蒙特塞拉特
            1721,//圣马丁
            1758,//圣卢西亚
            1767,//多米尼克
            1784,//圣文森特和格林纳丁斯
            1809, 829, 849,//多米尼加
            1868,//特立尼达和多巴哥
            1869,//圣基茨和尼维斯
            1876, 658,//牙买加
            1,//美国和加拿大，及其属地

            20,//埃及
            210,//西撒哈拉
            211,//南苏丹
            212,//摩洛哥
            213,//阿尔及利亚
            214,//未分配
            215,//未分配
            216,//突尼斯
            217,//未分配
            218,//利比亚
            219,//未分配
            220,//冈比亚
            221,//塞内加尔
            222,//毛里塔尼亚
            223,//马里
            224,//几内亚
            225,//科特迪瓦
            226,//布基纳法索
            227,//尼日尔
            228,//多哥
            229,//贝宁
            230,//毛里求斯
            231,//利比里亚
            232,//塞拉利昂
            233,//加纳
            234,//尼日利亚
            235,//乍得
            236,//中非
            237,//喀麦隆
            238,//佛得角
            239,//圣多美和普林西比
            240,//赤道几内亚
            241,//加蓬
            242,//刚果（布）
            243,//刚果（金）（即前扎伊尔）
            244,//安哥拉
            245,//几内亚比绍
            246,//英属印度洋领地
            247,//阿森松岛
            248,//塞舌尔
            249,//苏丹
            250,//卢旺达
            251,//埃塞俄比亚
            252,//索马里
            253,//吉布提
            254,//肯尼亚
            255,//坦桑尼亚
            256,//乌干达
            257,//布隆迪
            258,//莫桑比克
            259,//桑给巴尔从未使用――参见255坦桑尼亚
            260,//赞比亚
            261,//马达加斯加
            262,//留尼汪和马约特
            263,//津巴布韦
            264,//纳米比亚
            265,//马拉维
            266,//莱索托
            267,//博茨瓦纳
            268,//斯威士兰
            269,//科摩罗
            27,//南非
            290,//Template:Countrydata圣赫勒拿和特里斯坦达库尼亚
            291,//厄立特里亚
            292,//未分配
            293,//未分配
            294,//未分配
            295,//未分配
            296,//未分配
            297,//阿鲁巴
            298,//法罗群岛法罗群岛
            299,//格陵兰

            30,//希腊
            31,//荷兰
            32,//比利时
            33,//法国
            34,//西班牙
            350,//直布罗陀
            351,//葡萄牙
            352,//卢森堡
            353,//爱尔兰
            354,//冰岛
            355,//阿尔巴尼亚
            356,//马耳他
            357,//塞浦路斯
            358,//芬兰
            35818,//奥兰
            359,//保加利亚
            36,//匈牙利
            37,//不再使用，之前被分配给原东德，
            370,//立陶宛
            371,//拉脱维亚
            372,//爱沙尼亚
            373,//摩尔多瓦摩尔多瓦
            373,//德涅斯特河沿岸
            374,//亚美尼亚
            37447,//阿尔扎赫
            375,//白俄罗斯
            376,//安道尔
            377,//摩纳哥
            378,//圣马力诺
            379,//梵蒂冈
            38,//不再使用，之前被分配给原南斯拉夫
            380,//乌克兰
            381,//塞尔维亚
            382,//黑山
            383,//科索沃
            384,//未分配
            385,//克罗地亚
            386,//斯洛文尼亚
            387,//波斯尼亚和黑塞哥维那
            388,//欧洲电话号码空间――环欧洲服务
            389,//北马其顿
            39,//意大利

            40,//罗马尼亚
            41,//瑞士
            42,//不再使用，之前被分配给原捷克斯洛伐克
            420,//捷克
            421,//斯洛伐克
            422,//未分配
            423,//列支敦士登
            424,//未分配
            425,//未分配
            426,//未分配
            427,//未分配
            428,//未分配
            429,//未分配
            43,//奥地利
            441481,//根西
            441534,//泽西
            441624,//马恩岛
            44,//英国
            45,//丹麦
            46,//瑞典
            4779,//斯瓦尔巴和扬马延
            47,//挪威
            48,//波兰
            49,//德国

            500,//福克兰群岛和南乔治亚和南桑威奇群岛
            501,//伯利兹
            502,//危地马拉
            503,//萨尔瓦多
            504,//洪都拉斯
            505,//尼加拉瓜
            506,//哥斯达黎加
            507,//巴拿马
            508,//圣皮埃尔和密克隆
            509,//海地
            51,//秘鲁
            52,//墨西哥
            53,//古巴
            54,//阿根廷
            55,//巴西
            56,//智利
            57,//哥伦比亚
            58,//委内瑞拉
            590,//瓜德罗普
            591,//玻利维亚
            592,//圭亚那
            593,//厄瓜多尔
            594,//法属圭亚那
            595,//巴拉圭
            596,//马提尼克
            597,//苏里南
            598,//乌拉圭
            5993,//圣尤斯特歇斯
            5994,//萨巴
            5997,//博奈尔
            5999,//库拉索
            599,//原荷属安的列斯

            60,//马来西亚
            61,//澳大利亚科科斯（基林）群岛圣诞岛
            62,//印尼
            63,//菲律宾
            64,//新西兰新西兰皮特凯恩群岛
            65,//新加坡
            66,//泰国
            670,//东帝汶
            671,//曾经是关岛（现在是1671）
            672,//澳大利亚海外领地,//南极洲诺福克岛赫德岛和麦克唐纳群岛
            673,//文莱
            674,//瑙鲁
            675,//巴布亚新几内亚
            676,//汤加
            677,//所罗门群岛
            678,//瓦努阿图
            679,//斐济
            680,//帕劳
            681,//瓦利斯和富图纳
            682,//库克群岛
            683,//纽埃
            684,//曾经是美属萨摩亚（现在是1684）
            685,//萨摩亚
            686,//基里巴斯
            687,//新喀里多尼亚
            688,//图瓦卢
            689,//法属波利尼西亚
            690,//托克劳
            691,//密克罗尼西亚联邦密克罗尼西亚联邦
            692,//马绍尔群岛
            767,//哈萨克斯坦
            7,//俄罗斯

            800,//国际免费电话
            808,//保留给国际费用分担业务
            809,//未分配
            81,//日本
            82,//大韩民国
            83,//未分配
            84,//越南
            850,//朝鲜民主主义人民共和国
            851,//测试专用
            854,//未分配
            855,//柬埔寨
            856,//老挝
            870,//Inmarsat"SNAC"卫星电话
            871,//未分配（2008年前曾用于Inmarsat东大西洋区）
            872,//未分配（2008年前曾用于Inmarsat太平洋区）
            873,//未分配（2008年前曾用于Inmarsat印度洋区）
            874,//未分配（2008年前曾用于Inmarsat西大西洋区）
            875,//预留给海洋移动通信服务
            876,//预留给海洋移动通信服务
            877,//预留给海洋移动通信服务
            878,//环球个人通讯服务
            879,//预留给国家移动通信/海洋通信使用
            880,//孟加拉国孟加拉国
            881,//移动卫星系统
            882,//国际网络
            883,//国际网络
            884,//未分配
            885,//未分配
            887,//未分配
            888,//联合国人权事务协调办公室

            90392,//北塞浦路斯
            90,//土耳其
            91,//印度
            92,//巴基斯坦
            93,//阿富汗
            94,//斯里兰卡
            95,//缅甸
            960,//马尔代夫
            961,//黎巴嫩
            962,//约旦
            963,//叙利亚
            964,//伊拉克
            965,//科威特
            966,//沙特阿拉伯
            967,//也门
            968,//阿曼
            969,//不再使用，之前被分配给原南也门
            970,//巴勒斯坦
            971,//阿联酋
            972,//以色列
            973,//巴林
            974,//卡塔尔
            975,//不丹
            976,//蒙古国
            977,//尼泊尔
            978,//未分配
            979,//InternationalPremiumRateService
            98,//伊朗
            990,//未分配
            991,//InternationalTelecommunicationsPublicCorrespondenceServicetrial(ITPCS)
            992,//塔吉克斯坦
            993,//土库曼斯坦
            994,//阿塞拜疆
            995,//格鲁吉亚
            99534,//南奥塞梯
            99544,//阿布哈兹
            996,//吉尔吉斯斯坦
            997,//未分配
            998//乌兹别克斯坦
    };


    /**
     * 获取通讯录姓名
     *
     * @param context
     * @param number
     * @return
     */
    public static String getContactByNumber(Context context, String number) {
        if (TextUtils.isEmpty(number)) {
            Log.i(TAG, "number is empty");
            return "";
        }

        //如果是不是号码
        if (!Pattern.matches(REGEX_NUMBER, number)) {
            return number;
        }

        if (ActivityCompat.checkSelfPermission(context, READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            String finalContact = getContentUserName(context, number);
            if (!TextUtils.isEmpty(finalContact)) {
                return finalContact;
            }
        } else {
            Log.i(TAG, "READ_CONTACTS permission is not granted");
        }
        return number;
    }

    @Deprecated
    private static String doContactByNumber(Context context, String number) {
        String finalContact = "";
        Cursor cursor = null;
        try {
            String[] cols = {ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER};
            cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, cols, null, null, null);
            assert cursor != null;
            for (int i = 0; i < cursor.getCount(); i++) {
                cursor.moveToPosition(i);
                int nameFieldColumnIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME);
                int numberFieldColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                String itemName = cursor.getString(nameFieldColumnIndex);
                String itemNumber = cursor.getString(numberFieldColumnIndex);
                itemNumber = itemNumber.replaceAll("\\s*", "");
                if (itemNumber.equalsIgnoreCase(number)) {
                    finalContact = itemName;
                    break;
                }
            }
        } catch (Exception e) {
        } finally {
            assert cursor != null;
            cursor.close();
        }

        return finalContact;
    }


    /**
     * 查询避免由于区号引起拿不到联系人姓名的问题。（部分手机）
     *
     * @param number
     * @return
     */
    private static String getContentUserName(Context context, String number) {

        //去掉电话号码中的括号，破折号和空格
        String receiveNumber = number.replaceAll("[()\\s-]+", "");
        Log.i(TAG, "receiveNumber: " + receiveNumber);

        Cursor cursor = context.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{
                        ContactsContract.PhoneLookup.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER}, null, null, null);
        String contactName;
        String contactNumber;
        try {

            while (cursor != null && cursor.moveToNext()) {
                contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                contactNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)).replaceAll("[()\\s-]+", "");


                if (TextUtils.isEmpty(contactNumber)) {
                    continue;
                }

//                Log.i(TAG, "contactNumber: " + contactNumber + " contactName: " + contactName);

                //来电与通讯录都带区号，则整体做对比
                if (receiveNumber.startsWith("+") && contactNumber.startsWith("+")) {
                    if (receiveNumber.equals(contactNumber)) {
                        Log.i(TAG, "both have area code and matching,number: " + receiveNumber);
                        return contactName;
                    }
                }

                //来电与通讯录都不带区号，也整体做对比
                else if (!receiveNumber.startsWith("+") && !contactNumber.startsWith("+")) {
                    if (receiveNumber.equals(contactNumber)) {
                        Log.i(TAG, "both have no area code and matching,number: " + receiveNumber);
                        return contactName;
                    }
                }

                //来电带区号，但是通讯录不带区号
                else if (receiveNumber.startsWith("+") && !contactNumber.startsWith("+")) {
                    //部分手机在电话号码能够填写*#+-/等符号，先去掉非数字字符
                    String newNumber = receiveNumber.replaceAll("[\\D]", "");
                    String realNewNumber = replaceCountryCode(newNumber);
                    if (realNewNumber.equals(contactNumber)) {
                        Log.i(TAG, "receiveNumber have area code and matching,number: " + receiveNumber + "contactNumber: " + contactNumber);
                        return contactName;
                    }
                }

                //来电不带区号，但是通讯录带区号
                else if (!receiveNumber.startsWith("+") && contactNumber.startsWith("+")) {
                    String newContact = contactNumber.replaceAll("[\\D]", "");
                    String realNewContact = replaceCountryCode(newContact);
                    if (receiveNumber.equals(realNewContact)) {
                        Log.i(TAG, "contactNumber have area code and matching,number: " + receiveNumber + "contactNumber: " + contactNumber);
                        return contactName;
                    }
                }

            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        } finally {
            try {
                if (cursor != null) {
                    cursor.close();
                }
            } catch (Exception e) {
                Log.e(TAG, e.toString());
            }
        }
        return "";
    }


    /**
     * 匹配国家区号
     *
     * @param number 已经去掉了+号
     * @return
     */
    private static String replaceCountryCode(String number) {
        if (TextUtils.isEmpty(number)) {
            return "";
        }
        //没有匹配到任何区号则返回原号码
        String result = number;
        for (int code : countryCodes) {
            if (number.startsWith(String.valueOf(code))) {
                result = number.replaceFirst(String.valueOf(code), "");
                break;
            }
        }
        return result;
    }

}
