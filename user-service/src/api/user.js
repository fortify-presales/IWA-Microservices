const UserService = require('../services/user-service');
const  UserAuth = require('./middlewares/auth');
const { SubscribeMessage } = require('../utils');

module.exports = (app, channel) => {
    
    const service = new UserService();

    SubscribeMessage(channel, service);

    app.post('/user/register', async (req,res,next) => {
        const { email, password, phone } = req.body;
        const { data } = await service.Register({ email, password, phone}); 
        res.json(data);
    });

    app.post('/user/login', async (req,res,next) => {
        const { email, password } = req.body;
        const { data } = await service.SignIn({ email, password});
        res.json(data);
    });

    app.post('/user/address', UserAuth, async (req,res,next) => {
        const { _id } = req.user;
        const { street, postalCode, city, country } = req.body;
        const { data } = await service.AddNewAddress( _id , { street, postalCode, city,country});
        res.json(data);
    });

    app.get('/user/profile', UserAuth ,async (req,res,next) => {
        const { _id } = req.user;
        const { data } = await service.GetProfile({ _id });
        res.json(data);
    });
     
    app.get('/user/wishlist', UserAuth, async (req,res,next) => {
        const { _id } = req.user;
        const { data } = await service.GetWishList(_id);
        return res.status(200).json(data);
    });

    //

    app.get("/user/health", (req, res) => {
        return res.status(200).json({ msg: "OK" });
    });

    app.get('/user/whoami', (req,res,next) => {
        return res.status(200).json({msg: '/user : I am the Users Service'})
    })
}
